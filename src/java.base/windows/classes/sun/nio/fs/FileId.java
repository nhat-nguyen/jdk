package sun.nio.fs;

import jdk.internal.misc.Unsafe;

import java.util.Arrays;

import static sun.nio.fs.WindowsNativeDispatcher.GetFileInformationByHandleEx_FileIdInfo;

/**
 * Windows implementation of DosFileAttributes/BasicFileAttributes
 */

class FileId {
    private static final Unsafe unsafe = Unsafe.getUnsafe();

    /*
     * typedef struct _FILE_ID_INFO {
     *     ULONGLONG VolumeSerialNumber;
     *     FILE_ID_128 FileId;
     * } FILE_ID_INFO;
     *
     * typedef struct _FILE_ID_128 {
     *     BYTE  Identifier[16];
     * } FILE_ID_128;
     */
    private static final short SIZEOF_FILE_ID_INFO = 24;
    private static final short OFFSETOF_FILE_ID_INFO_VOLSERIALNUM = 0;
    private static final short OFFSETOF_FILE_ID_INFO_FILEID = 8;

    private final long volSerialNumber;
    private final byte[] fileId;

    private FileId(long volSerialNumber, byte[] fileId) {
        this.volSerialNumber = volSerialNumber;
        this.fileId = fileId;
    }

    static FileId readFileIdInfo(long handle) throws WindowsException {
        NativeBuffer buffer = NativeBuffers.getNativeBuffer(SIZEOF_FILE_ID_INFO);
        try {
            long address = buffer.address();
            GetFileInformationByHandleEx_FileIdInfo(handle, address);
            return fromFileId(address);
        } finally {
            buffer.release();
        }
    }

    static boolean isSameFile(FileId fileId1, FileId fileId2) {
        return fileId1.volSerialNumber == fileId2.volSerialNumber &&
                Arrays.equals(fileId1.fileId, fileId2.fileId);
    }

    private static FileId fromFileId(long address) {
        long volSerialNumber = unsafe.getLong(address + OFFSETOF_FILE_ID_INFO_VOLSERIALNUM);
        byte[] fieldId = new byte[16];
        for (int offset = 0; offset < 16; offset++) {
            fieldId[offset] = unsafe.getByte(address + OFFSETOF_FILE_ID_INFO_FILEID + offset);
        }
        return new FileId(volSerialNumber, fieldId);
    }
}
