package demo;

import feign.Target;

public class AppTarget<T> extends Target.HardCodedTarget<T> {

    private String group ;

    public AppTarget(String group , Class<T> type, String url) {
        super(type, url);
        this.group = group ;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
