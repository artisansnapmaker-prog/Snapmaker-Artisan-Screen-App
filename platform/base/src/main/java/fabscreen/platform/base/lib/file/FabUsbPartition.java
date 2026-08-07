package fabscreen.platform.base.lib.file;

import android.content.Context;
import android.os.StatFs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import fabscreen.platform.lib.LogHelper;


public class FabUsbPartition extends AbstractPartition {
    private Context mContext;
    private String mUuid;
    private String mRootPath;

    // Partition only created when device permission granted
    public FabUsbPartition(Context context, String rootPath, String uuid) {
        mContext = context;
        mRootPath = rootPath;
        mUuid = uuid;
        init();
    }

    public String getUuid() {
        return mUuid;
    }

    public StatFs getStaFs() {
        StatFs statFs = null;
        try {
            statFs = new StatFs(mRootPath);
        } catch (Exception ignored) {

        }
        return statFs;
    }

    @Override
    public IFile getRootFile() {
        return new FabUsbFile(new File(mRootPath));
    }

    @Override
    public long getUsedSpace() {
        StatFs staFs = getStaFs();
        if (staFs == null) return 0;
        return staFs.getTotalBytes() - staFs.getFreeBytes();
    }

    @Override
    public long getFreeSpace() {
        StatFs staFs = getStaFs();
        if (staFs == null) return 0;
        return staFs.getFreeBytes();
    }

    @Override
    public long getTotalSpace() {
        StatFs staFs = getStaFs();
        if (staFs == null) return 0;
        return staFs.getTotalBytes();
    }

    @Override
    public IFile search(String path) {
        if (path.startsWith(mRootPath)) {
            path = path.substring(mRootPath.length());
        }

        if ("/".equals(path)) {
            return getRootFile();
        }
        try {
            if (!path.startsWith("/")) {
                ArrayList<IFile> files = getCurrentDirectory().listFiles();
                for (IFile file : files) {
                    if (path.equals(file.getName())) {
                        return file;
                    }
                }
                return null;
            } else {
                return search(getRootFile(), path);
            }
        } catch (Exception e) {
            LogHelper.log(e);
            return null;
        }
    }

    private IFile search(IFile nowFile, String path) throws IOException {
        if (path.isEmpty()) return nowFile;
        ArrayList<IFile> files = nowFile.listFiles();
        int startIndex = path.indexOf(File.separatorChar);
        int endIndex = path.indexOf(File.separatorChar, startIndex + 1);
        String pathName = path;
        if (endIndex != -1) {
            pathName = path.substring(startIndex + 1, endIndex);
        } else if (startIndex != -1) {
            pathName = path.substring(startIndex + 1);
        }
        for (IFile file : files) {
            if (pathName.equals(file.getName())) {
                if (endIndex == -1) {
                    return file;
                }
                return search(file, path.substring(endIndex));
            }
        }
        return null;
    }

    /**
     * If the init is unsuccessful, the resulting mStack may be 0
     *
     * @return
     */
    @Override
    public boolean isRoot() {
        return mStack.size() == 1;
    }

    @Override
    public IFile getCurrentDirectory() {
        return mStack.isEmpty() ? getRootFile() : mStack.peek();
    }

    @Override
    public void gotoDirectory(IFile file) {
        mStack.push(file);
    }

    @Override
    public void popDirectory() {
        if (mStack.size() > 1)
            mStack.pop();
    }

    @Override
    public IFile createFile(IFile file, String name) throws IOException {
        return file.createFile(name);
    }

    @Override
    public IFile createDirectory(IFile file, String name) throws IOException {
        return file.createDirectory(name);
    }

    @Override
    public void removeFile(IFile file) throws IOException {
        file.removeFile();
    }

    @Override
    public String toString() {
        return "FabUsbPartition{" +
                "mUuid='" + mUuid + '\'' +
                ", mRootPath='" + mRootPath + '\'' +
                '}';
    }

    @Override
    public void renameFile(IFile file, String name) throws IOException {
        file.renameFile(name);

    }
}
