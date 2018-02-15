package ar.arsensors.model;

public class ModelObject {  

    private Integer id;
    private Integer x1;
    private Integer y1;
    private Integer x2;
    private Integer y2;
    private String caption;
    private Integer visible;
    private Long tsVisible;
    private String descr;

    public Integer getId() {
        return id;
    }

    public Integer getX1() {
        return x1;
    }

    public Integer getX2() {
        return x2;
    }

    public Integer getY1() {
        return y1;
    }

    public Integer getY2() {
        return y2;
    }

    public String getCaption() {
        return caption;
    }

    public String getDescription() {
        return descr;
    }

    public Integer getVisibility() {
        return visible;
    }

    public void setVisible(Integer visible) {
        this.visible = visible;
    }

    public Long getTsVisibility() {
        return tsVisible;
    }

    public void setTsVisible(Long tsVisible) {
        this.tsVisible = tsVisible;
    }

    public ModelObject(Integer id, Integer x1, Integer y1, Integer x2, Integer y2, String caption, String descr, Integer visible, Long tsVisible) {
        this.id = id;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.caption = caption;
        this.visible = visible;
        this.tsVisible = tsVisible;
        this.descr = descr;
    }

}