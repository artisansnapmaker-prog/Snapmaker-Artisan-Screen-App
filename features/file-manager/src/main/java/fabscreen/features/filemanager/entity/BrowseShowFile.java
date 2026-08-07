package fabscreen.features.filemanager.entity;

import fabscreen.platform.base.lib.file.IFile;

public class BrowseShowFile {
    private final FileType mFileType;
    private String mThumbnailFilePath;
    private int mDefaultDisplayResId;
    private IFile mFile;
    private boolean mIsHaveThumbnail = false;
    private boolean mIsSetView = false;
    private boolean mIsSelect;


    public BrowseShowFile(FileType fileType, IFile iFile, int defaultDisplayResId) {
        mFileType = fileType;
        mDefaultDisplayResId = defaultDisplayResId;
        mFile = iFile;
    }

    public FileType getFileType() {
        return mFileType;
    }

    public IFile getIFile() {
        return mFile;
    }

    public String getThumbnailFilePath() {
        return mThumbnailFilePath;
    }

    public void setThumbnailPath(String extractFilePath) {
        mThumbnailFilePath = extractFilePath;
        mIsHaveThumbnail = true;
    }

    public boolean haveThumbnail() {
        return mIsHaveThumbnail;
    }

    public boolean isSetView() {
        return mIsSetView;
    }

    public void setIsSetView(boolean isSetView) {
        mIsSetView = isSetView;
    }

    public int getDefaultDisplay() {
        return mDefaultDisplayResId;
    }

    public boolean isSelect() {
        return mIsSelect;
    }

    public void setSelect(boolean isSelect) {
        mIsSelect = isSelect;
    }
}
