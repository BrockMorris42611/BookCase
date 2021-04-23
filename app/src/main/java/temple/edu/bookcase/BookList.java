package temple.edu.bookcase;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class BookList implements Parcelable {

    private ArrayList<Book> Library;
    private Book chosenBook;

    public BookList(ArrayList<Book> Library) {
        this.Library = Library;
        chosenBook = null;
    }

    public int getSize() { return Library.size(); }
    public Book getChosenBook(){return chosenBook;}
    public ArrayList<Book> getLibrary() { return Library; }
    public void setLibrary(ArrayList<Book> library) { Library = library; }
    public void setChosenBook(Book b){chosenBook = b;}

    protected BookList(Parcel in) {
        Library = in.createTypedArrayList(Book.CREATOR);
    }
    public static final Creator<BookList> CREATOR = new Creator<BookList>() {
        @Override
        public BookList createFromParcel(Parcel in) {
            return new BookList(in);
        }

        @Override
        public BookList[] newArray(int size) {
            return new BookList[size];
        }
    };
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(Library);
        dest.writeParcelable(chosenBook, flags);
    }
}
class Book implements Parcelable{

    private int id;
    private int duration;
    //private int current_progress;
    private String title;
    private String author;
    private String coverURL;

    public Book(int id, int duration, /*int current_progress,*/ String title, String author, String coverURL){
        this.id = id;
        this.duration = duration;
        this.title = title;
        this.author = author;
        this.coverURL = coverURL;
        //this.current_progress = current_progress;
    }

    protected Book(Parcel in) {
        id = in.readInt();
        duration = in.readInt();
        //current_progress = in.readInt();
        title = in.readString();
        author = in.readString();
        coverURL = in.readString();
    }
    public static final Creator<Book> CREATOR = new Creator<Book>() {
        @Override
        public Book createFromParcel(Parcel in) { return new Book(in); }
        @Override
        public Book[] newArray(int size) { return new Book[size]; }};
    @Override
    public int describeContents() { return 0; }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(duration);
        //dest.writeInt(current_progress);
        dest.writeString(title);
        dest.writeString(author);
        dest.writeString(coverURL); }

    public int getId() { return id; }
    public String getCoverURL() { return coverURL; }
    public String getTitle() {
        return title;
    }
    public String getAuthor() {
        return author;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public void setId(int id) { this.id = id; }
    public void setCoverURL(String coverURL) { this.coverURL = coverURL; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    //public void setCurrent_progress(int c){ this.current_progress = c;}
    //public int getCurrentProgress(){return current_progress;}
    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", coverURL='" + coverURL + '\'' +
                '}';
    }
}