package fabscreen.platform.core.ui.view;

public class HelpBean {

    private int mPicResource;
    private String mContent;

    public HelpBean(int picResource, String content) {
        mPicResource = picResource;
        mContent = content;
    }

    public int getPicResource() {
        return mPicResource;
    }

    public void setPicResource(int mPicResource) {
        this.mPicResource = mPicResource;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String mContent) {
        this.mContent = mContent;
    }

}
