/**
 * Copyright 2012-2019 The Feign Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package feign;

import feign.Request.HttpMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;

/**
 * Defines what annotations and values are valid on interfaces.
 */
public interface Contract {

  /**
   * Called to parse the methods in the class that are linked to HTTP requests.
   *
   * @param targetType {@link feign.Target#type() type} of the Feign interface.
   */
  // TODO: break this and correct spelling at some point
  List<MethodMetadata> parseAndValidatateMetadata(Class<?> targetType);

  abstract class BaseContract implements Contract {

    @Override
    public List<MethodMetadata> parseAndValidatateMetadata(Class<?> targetType) {
      checkState(targetType.getTypeParameters().length == 0, "Parameterized types unsupported: %s",
          targetType.getSimpleName());
      checkState(targetType.getInterfaces().length <= 1, "Only single inheritance supported: %s",
          targetType.getSimpleName());
      if (targetType.getInterfaces().length == 1) {
        checkState(targetType.getInterfaces()[0].getInterfaces().length == 0,
            "Only single-level inheritance supported: %s",
            targetType.getSimpleName());
      }
      Map<String, MethodMetadata> result = new LinkedHashMap<String, MethodMetadata>();
      for (Method method : targetType.getMethods()) {
        // 不对Object对象内方法、静态方法 和 默认实现方法进行处理
        if (method.getDeclaringClass() == Object.class ||
            (method.getModifiers() & Modifier.STATIC) != 0 ||
            Util.isDefault(method)) {
          continue;
        }
        // 解析方法元数据
        MethodMetadata metadata = parseAndValidateMetadata(targetType, method);
        // 不能存在相同的类名和方法名称
        checkState(!result.containsKey(metadata.configKey()), "Overrides unsupported: %s",
            metadata.configKey());
        result.put(metadata.configKey(), metadata);
      }
      return new ArrayList<>(result.values());
    }

    /**
     * @deprecated use {@link #parseAndValidateMetadata(Class, Method)} instead.
     */
    @Deprecated
    public MethodMetadata parseAndValidatateMetadata(Method method) {
      return parseAndValidateMetadata(method.getDeclaringClass(), method);
    }

    /**
     * Called indirectly by {@link #parseAndValidatateMetadata(Class)}.
     */
    protected MethodMetadata parseAndValidateMetadata(Class<?> targetType, Method method) {
      MethodMetadata data = new MethodMetadata();
      // 返回值类型
      data.returnType(Types.resolve(targetType, targetType, method.getGenericReturnType()));
      // 方法调用的唯一key
      data.configKey(Feign.configKey(targetType, method));

      // 只有一个接口，则解析父接口中的注解
      if (targetType.getInterfaces().length == 1) {
        processAnnotationOnClass(data, targetType.getInterfaces()[0]);
      }
      // 解析在当前类上的注解
      processAnnotationOnClass(data, targetType);

      // 解析在当前方法上的注解
      for (Annotation methodAnnotation : method.getAnnotations()) {
        processAnnotationOnMethod(data, methodAnnotation, method);
      }
      checkState(data.template().method() != null,
          "Method %s not annotated with HTTP method type (ex. GET, POST)",
          method.getName());
      Class<?>[] parameterTypes = method.getParameterTypes();
      Type[] genericParameterTypes = method.getGenericParameterTypes();

      Annotation[][] parameterAnnotations = method.getParameterAnnotations();
      int count = parameterAnnotations.length;
      for (int i = 0; i < count; i++) {
        boolean isHttpAnnotation = false;
        // 处理每一个请求参数注解, @Param , @QueryMap , @HeaderMap 为 HttpAnnotation
        if (parameterAnnotations[i] != null) {
          isHttpAnnotation = processAnnotationsOnParameter(data, parameterAnnotations[i], i);
        }
        // 对于URI类型的参数，单独进行处理
        if (parameterTypes[i] == URI.class) {
          data.urlIndex(i);
        } else if (!isHttpAnnotation && parameterTypes[i] != Request.Options.class) {
          // 请求方法中只能有一个不带注解的变量，将其作为请求body
          checkState(data.formParams().isEmpty(),
              "Body parameters cannot be used with form parameters.");
          checkState(data.bodyIndex() == null, "Method has too many Body parameters: %s", method);
          data.bodyIndex(i);
          data.bodyType(Types.resolve(targetType, targetType, genericParameterTypes[i]));
        }
      }

      if (data.headerMapIndex() != null) {
        checkMapString("HeaderMap", parameterTypes[data.headerMapIndex()],
            genericParameterTypes[data.headerMapIndex()]);
      }

      if (data.queryMapIndex() != null) {
        if (Map.class.isAssignableFrom(parameterTypes[data.queryMapIndex()])) {
          checkMapKeys("QueryMap", genericParameterTypes[data.queryMapIndex()]);
        }
      }

      return data;
    }

    private static void checkMapString(String name, Class<?> type, Type genericType) {
      checkState(Map.class.isAssignableFrom(type),
          "%s parameter must be a Map: %s", name, type);
      checkMapKeys(name, genericType);
    }

    private static void checkMapKeys(String name, Type genericType) {
      Class<?> keyClass = null;

      // assume our type parameterized
      if (ParameterizedType.class.isAssignableFrom(genericType.getClass())) {
        Type[] parameterTypes = ((ParameterizedType) genericType).getActualTypeArguments();
        keyClass = (Class<?>) parameterTypes[0];
      } else if (genericType instanceof Class<?>) {
        // raw class, type parameters cannot be inferred directly, but we can scan any extended
        // interfaces looking for any explict types
        Type[] interfaces = ((Class) genericType).getGenericInterfaces();
        if (interfaces != null) {
          for (Type extended : interfaces) {
            if (ParameterizedType.class.isAssignableFrom(extended.getClass())) {
              // use the first extended interface we find.
              Type[] parameterTypes = ((ParameterizedType) extended).getActualTypeArguments();
              keyClass = (Class<?>) parameterTypes[0];
              break;
            }
          }
        }
      }

      if (keyClass != null) {
        checkState(String.class.equals(keyClass),
            "%s key must be a String: %s", name, keyClass.getSimpleName());
      }
    }


    /**
     * Called by parseAndValidateMetadata twice, first on the declaring class, then on the target
     * type (unless they are the same).
     *
     * @param data metadata collected so far relating to the current java method.
     * @param clz the class to process
     */
    protected abstract void processAnnotationOnClass(MethodMetadata data, Class<?> clz);

    /**
     * @param data metadata collected so far relating to the current java method.
     * @param annotation annotations present on the current method annotation.
     * @param method method currently being processed.
     */
    protected abstract void processAnnotationOnMethod(MethodMetadata data,
                                                      Annotation annotation,
                                                      Method method);

    /**
     * @param data metadata collected so far relating to the current java method.
     * @param annotations annotations present on the current parameter annotation.
     * @param paramIndex if you find a name in {@code annotations}, call
     *        {@link #nameParam(MethodMetadata, String, int)} with this as the last parameter.
     * @return true if you called {@link #nameParam(MethodMetadata, String, int)} after finding an
     *         http-relevant annotation.
     */
    protected abstract boolean processAnnotationsOnParameter(MethodMetadata data,
                                                             Annotation[] annotations,
                                                             int paramIndex);

    /**
     * links a parameter name to its index in the method signature.
     */
    protected void nameParam(MethodMetadata data, String name, int i) {
      Collection<String> names =
          data.indexToName().containsKey(i) ? data.indexToName().get(i) : new ArrayList<String>();
      names.add(name);
      data.indexToName().put(i, names);
    }
  }

  class Default extends BaseContract {

    static final Pattern REQUEST_LINE_PATTERN = Pattern.compile("^([A-Z]+)[ ]*(.*)$");

    @Override
    protected void processAnnotationOnClass(MethodMetadata data, Class<?> targetType) {
      if (targetType.isAnnotationPresent(Headers.class)) {
        String[] headersOnType = targetType.getAnnotation(Headers.class).value();
        checkState(headersOnType.length > 0, "Headers annotation was empty on type %s.",
            targetType.getName());
        Map<String, Collection<String>> headers = toMap(headersOnType);
        headers.putAll(data.template().headers());
        data.template().headers(null); // to clear
        data.template().headers(headers);
      }
    }

    @Override
    protected void processAnnotationOnMethod(MethodMetadata data,
                                             Annotation methodAnnotation,
                                             Method method) {
      Class<? extends Annotation> annotationType = methodAnnotation.annotationType();
      if (annotationType == RequestLine.class) {
        String requestLine = RequestLine.class.cast(methodAnnotation).value();
        checkState(emptyToNull(requestLine) != null,
            "RequestLine annotation was empty on method %s.", method.getName());

        Matcher requestLineMatcher = REQUEST_LINE_PATTERN.matcher(requestLine);
        if (!requestLineMatcher.find()) {
          throw new IllegalStateException(String.format(
              "RequestLine annotation didn't start with an HTTP verb on method %s",
              method.getName()));
        } else {
          data.template().method(HttpMethod.valueOf(requestLineMatcher.group(1)));
          data.template().uri(requestLineMatcher.group(2));
        }
        // 是否对反斜杠进行处理
        data.template().decodeSlash(RequestLine.class.cast(methodAnnotation).decodeSlash());
        // 集合格式处理
        data.template()
            .collectionFormat(RequestLine.class.cast(methodAnnotation).collectionFormat());

      } else if (annotationType == Body.class) {
        String body = Body.class.cast(methodAnnotation).value();
        checkState(emptyToNull(body) != null, "Body annotation was empty on method %s.",
            method.getName());
        if (body.indexOf('{') == -1) {
          data.template().body(body);
        } else {
          data.template().bodyTemplate(body);
        }
      } else if (annotationType == Headers.class) {
        String[] headersOnMethod = Headers.class.cast(methodAnnotation).value();
        checkState(headersOnMethod.length > 0, "Headers annotation was empty on method %s.",
            method.getName());
        data.template().headers(toMap(headersOnMethod));
      }
    }

    @Override
    protected boolean processAnnotationsOnParameter(MethodMetadata data,
                                                    Annotation[] annotations,
                                                    int paramIndex) {
      boolean isHttpAnnotation = false;
      // 针对单个参数的所有注解进行处理
      for (Annotation annotation : annotations) {
        Class<? extends Annotation> annotationType = annotation.annotationType();
        if (annotationType == Param.class) {
          Param paramAnnotation = (Param) annotation;
          String name = paramAnnotation.value();
          checkState(emptyToNull(name) != null, "Param annotation was empty on param %s.",
              paramIndex);
          // 按照数字序号添加参数名称
          nameParam(data, name, paramIndex);
          // 存储数字序号参数对应的值
          Class<? extends Param.Expander> expander = paramAnnotation.expander();
          if (expander != Param.ToStringExpander.class) {
            data.indexToExpanderClass().put(paramIndex, expander);
          }
          // 存储数字需要参数对应的
          data.indexToEncoded().put(paramIndex, paramAnnotation.encoded());
          isHttpAnnotation = true;
          if (!data.template().hasRequestVariable(name)) {
            data.formParams().add(name);
          }
        } else if (annotationType == QueryMap.class) {
          checkState(data.queryMapIndex() == null,
              "QueryMap annotation was present on multiple parameters.");
          data.queryMapIndex(paramIndex);
          data.queryMapEncoded(QueryMap.class.cast(annotation).encoded());
          isHttpAnnotation = true;
        } else if (annotationType == HeaderMap.class) {
          checkState(data.headerMapIndex() == null,
              "HeaderMap annotation was present on multiple parameters.");
          data.headerMapIndex(paramIndex);
          isHttpAnnotation = true;
        }
      }
      return isHttpAnnotation;
    }

    private static Map<String, Collection<String>> toMap(String[] input) {
      Map<String, Collection<String>> result =
          new LinkedHashMap<String, Collection<String>>(input.length);
      for (String header : input) {
        int colon = header.indexOf(':');
        String name = header.substring(0, colon);
        if (!result.containsKey(name)) {
          result.put(name, new ArrayList<String>(1));
        }
        result.get(name).add(header.substring(colon + 1).trim());
      }
      return result;
    }
  }
}
