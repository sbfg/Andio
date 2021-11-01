package android.andio;

import android.os.Parcel;
import android.os.Parcelable;
import java.io.File;

public final class Record implements Parcelable
{
    private final File file;
    private String name;
    private int duration;
    private final long dateTimeMillis;

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public long getDateTimeMillis() {
        return dateTimeMillis;
    }

    public Record(final File file,
                   final String name,
                   final int duration,
                   final long dateTimeMillis) {
        this.file = file;
        this.name = name;
        this.duration = duration;
        this.dateTimeMillis = dateTimeMillis;
    }

    protected Record(final Parcel in) {
        file = new File(in.readString());
        name = in.readString();
        duration = in.readInt();
        dateTimeMillis = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(file.getAbsolutePath());
        dest.writeString(name);
        dest.writeInt(duration);
        dest.writeLong(dateTimeMillis);
    }

    @Override
    public String toString() {
        return "Record{" +
                "file=" + file +
                ", name='" + name + '\'' +
                ", duration=" + duration +
                ", dateTimeMillis=" + dateTimeMillis +
                '}';
    }

    public static final Creator<Record> CREATOR = new Creator<Record>()
    {
        @Override
        public Record createFromParcel(Parcel in) {
            return new Record(in);
        }

        @Override
        public Record[] newArray(int size) {
            return new Record[size];
        }
    };
}
