package temple.edu.bookcase;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import edu.temple.audiobookplayer.AudiobookService;

public class MainActivity extends AppCompatActivity implements BookListFragment.BookSelectedInterface, ControlFragment.ControlInterface {

    private FragmentManager fm;

    private boolean twoPane;
    private  BookDetailsFragment bookDetailsFragment;
    private  ControlFragment controlFragment;
    private Book selectedBook, playingBook;

    private final int MAX_NUM_OF_BOOKS = 7; // number of books as per the website we are extracting from.

    File list_file; String list_file_name = "list_file"; //file and key used to restore the list               on reopen of app
    File book_file; String book_file_name = "book_file"; //file and key used to restore the selected book      on reopen of app
    File time_file; String time_file_name = "time_file"; //file and key used to restore the times of the books on reopen of app
    File downloaded_file; String downloaded_file_name = "downloaded_file"; //keeps track of what is downloaded and not

    boolean[] isDownloaded = new boolean[MAX_NUM_OF_BOOKS]; // keep track of which books are downloaded
    int[] rememberedTime = new int[MAX_NUM_OF_BOOKS]; //remembered start times for each book

    private final String TAG_BOOKLIST = "booklist", TAG_BOOKDETAILS = "bookdetails";
    private final String KEY_SELECTED_BOOK = "selectedBook", KEY_PLAYING_BOOK = "playingBook";
    private final String KEY_BOOKLIST = "searchedook";
    private final int BOOK_SEARCH_REQUEST_CODE = 123;

    private AudiobookService.MediaControlBinder mediaControl;
    private boolean serviceConnected;

    Intent serviceIntent;
    BookList bookList;

    Handler progressHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            // Don't update contols if we don't know what book the service is playing
            if (message.obj != null && playingBook != null) {
                controlFragment.updateProgress((int) (((float) ((AudiobookService.BookProgress) message.obj).getProgress() / playingBook.getDuration()) * 100));
                controlFragment.setNowPlaying(getString(R.string.now_playing, playingBook.getTitle()));
            }

            return true;
        }
    });

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mediaControl = (AudiobookService.MediaControlBinder) iBinder;
            mediaControl.setProgressHandler(progressHandler);
            serviceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceConnected = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceIntent = new Intent (this, AudiobookService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);


        fm = getSupportFragmentManager();

        list_file = new File(getFilesDir(), list_file_name); //this file is going to store all the booklist
        book_file = new File(getFilesDir(), book_file_name);
        time_file = new File(getFilesDir(), time_file_name);
        downloaded_file = new File(getFilesDir(), downloaded_file_name);

        Arrays.fill(rememberedTime, 0); //set all start times to zero unless stated otherwise
        Arrays.fill(isDownloaded, false);

        findViewById(R.id.searchDialogButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(MainActivity.this, BookSearchActivity.class), BOOK_SEARCH_REQUEST_CODE);
            }
        });

        if (savedInstanceState != null) {
            // Fetch selected book if there was one
            selectedBook = savedInstanceState.getParcelable(KEY_SELECTED_BOOK);
            // Fetch playing book if there was one
            playingBook = savedInstanceState.getParcelable(KEY_PLAYING_BOOK);
            // Fetch previously searched books if one was previously retrieved
            bookList = savedInstanceState.getParcelable(KEY_BOOKLIST);
            //Fetch times books left off at
            rememberedTime = savedInstanceState.getIntArray(time_file_name);
        }else {
            // Create empty booklist if
            bookList = new BookList();

        }
        loadInPreviousAppList();
        loadInPreviousAppBook();
        loadInPreviousAppTime();
        loadInPreviousDownload();
        twoPane = findViewById(R.id.container2) != null;

        Fragment fragment1;
        fragment1 = fm.findFragmentById(R.id.container_1);

        // I will only ever have a single ControlFragment - if I created one before, reuse it
        if ((controlFragment = (ControlFragment) fm.findFragmentById(R.id.control_container)) == null) {
            controlFragment = new ControlFragment();
            fm.beginTransaction()
                    .add(R.id.control_container, controlFragment)
                    .commit();
        }


        // At this point, I only want to have BookListFragment be displayed in container_1
        if (fragment1 instanceof BookDetailsFragment) {
            fm.popBackStack();
        } else if (!(fragment1 instanceof BookListFragment))
            fm.beginTransaction()
                    .add(R.id.container_1, BookListFragment.newInstance(bookList), TAG_BOOKLIST)
                    .commit();

        /*
        If we have two containers available, load a single instance
        of BookDetailsFragment to display all selected books
         */
        bookDetailsFragment = (selectedBook == null) ? new BookDetailsFragment() : BookDetailsFragment.newInstance(selectedBook);
        if (twoPane) {
            fm.beginTransaction()
                    .replace(R.id.container2, bookDetailsFragment, TAG_BOOKDETAILS)
                    .commit();
        } else if (selectedBook != null) {
            /*
            If a book was selected, and we now have a single container, replace
            BookListFragment with BookDetailsFragment, making the transaction reversible
             */
            fm.beginTransaction()
                    .replace(R.id.container_1, bookDetailsFragment, TAG_BOOKDETAILS)
                    .addToBackStack(null)
                    .commit();
        }

    }

    @Override
    public void bookSelected(int index) {
        // Store the selected book to use later if activity restarts
        selectedBook = bookList.get(index);
        FileOutputStream fos;
        try {
            fos = openFileOutput(book_file_name, MODE_PRIVATE);
            String s = "";
            s += selectedBook.getTitle() + '\t' + selectedBook.getAuthor() + '\t' + selectedBook.getId() + '\t' + selectedBook.getCoverUrl() + '\t' + selectedBook.getDuration() + '\t' + '\n';
            //System.out.println(s);
            fos.write(s.getBytes());
            fos.close();} catch (IOException e) {e.printStackTrace();}
        if (twoPane)
            bookDetailsFragment.displayBook(selectedBook);//Display selected book using previously attached fragment
        else {
            fm.beginTransaction()
                    .replace(R.id.container_1, BookDetailsFragment.newInstance(selectedBook), TAG_BOOKDETAILS)//Display book using new fragment
                    .addToBackStack(null)//Transaction is reversible
                    .commit();
        }
        int b_id = selectedBook.getId();
        File newAudioBook = new File(getFilesDir(), String.valueOf(b_id));
        if(!newAudioBook.exists()) {
            Intent z = new Intent(MainActivity.this, downloadIntentService.class);
            z.putExtra("book_id", b_id);
            startService(z);
            isDownloaded[b_id-1] = true;
        }
    }

    /**
     * Display new books when retrieved from a search
     */
    private void showNewBooks() {
        if ((fm.findFragmentByTag(TAG_BOOKDETAILS) instanceof BookDetailsFragment)) {
            fm.popBackStack();
        }
        ((BookListFragment) fm.findFragmentByTag(TAG_BOOKLIST)).showNewBooks();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_SELECTED_BOOK, selectedBook);
        outState.putParcelable(KEY_PLAYING_BOOK, playingBook);
        outState.putParcelable(KEY_BOOKLIST, bookList);
        outState.putIntArray(time_file_name, rememberedTime);
    }

    @Override
    public void onBackPressed() {
        // If the user hits the back button, clear the selected book
        selectedBook = null;
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BOOK_SEARCH_REQUEST_CODE && resultCode == RESULT_OK) {
            bookList.clear();
            bookList.addAll((BookList) data.getParcelableExtra(BookSearchActivity.BOOKLIST_KEY));
            if (bookList.size() == 0) {
                Toast.makeText(this, getString(R.string.error_no_results), Toast.LENGTH_SHORT).show();
            }
            FileOutputStream fos;
            try {
                fos = openFileOutput(list_file_name, MODE_PRIVATE);
                String s = "";
                Book b;
                for(int i = 0; i < bookList.size(); i++){
                    b = bookList.get(i);
                    s += b.getTitle() + '\t' + b.getAuthor() + '\t' + b.getId() + '\t' + b.getCoverUrl() + '\t' + b.getDuration() + '\t' + '\n';
                }
                System.out.println(s);
                fos.write(s.getBytes());
                fos.close(); } catch (IOException e){e.printStackTrace();}
            showNewBooks();
        }
    }
    void loadInPreviousAppList(){
        if(!list_file.exists())
            try { list_file.createNewFile();} catch (IOException e) { e.printStackTrace();}
        else{
            StringBuilder sb = new StringBuilder();
            String []read_file_woo;
            try{
                FileInputStream inputStream = new FileInputStream(list_file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    read_file_woo = line.split("\t");
                    for(int i = 0; i < read_file_woo.length; i++) // data is stored >> title author id Url Duration
                        System.out.println(read_file_woo[i]);
                    bookList.add(new Book(Integer.parseInt(read_file_woo[2]), read_file_woo[0], read_file_woo[1],read_file_woo[3], Integer.parseInt(read_file_woo[4])));
                    sb.append(line).append("\n");
                }
                inputStream.close(); }catch(OutOfMemoryError om){om.printStackTrace();}catch(Exception ex){ex.printStackTrace();}
            String result = sb.toString();
            System.out.println("\n" + result);
        }
        System.out.println(bookList.toString());
    }
    void loadInPreviousAppBook(){
        if(!book_file.exists())
            try { book_file.createNewFile();} catch (IOException e) { e.printStackTrace();}
        else{
            String []read_file_woo;
            try{
                FileInputStream inputStream = new FileInputStream(book_file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    read_file_woo = line.split("\t");
                    selectedBook = new Book(Integer.parseInt(read_file_woo[2]), read_file_woo[0], read_file_woo[1],read_file_woo[3], Integer.parseInt(read_file_woo[4]));
                    playingBook = selectedBook;
                }
                controlFragment.setNowPlaying(selectedBook.getTitle());
                inputStream.close(); }catch(OutOfMemoryError om){om.printStackTrace();}catch(Exception ex){ex.printStackTrace();}
        }
        System.out.println(bookList.toString());
    }
    void loadInPreviousAppTime(){
        if(!time_file.exists())
            try { time_file.createNewFile();} catch (IOException e) { e.printStackTrace();}
        else{
            String []read_file_woo;
            try{
                FileInputStream inputStream = new FileInputStream(time_file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    read_file_woo = line.split(" ");
                    for(int i = 0; i < read_file_woo.length; i++)// data is stored >> title author id Url Duration
                        rememberedTime[i] = Integer.parseInt(read_file_woo[i]);
                }
                inputStream.close(); }catch(OutOfMemoryError om){om.printStackTrace();}catch(Exception ex){ex.printStackTrace();}
        }
    }
    void loadInPreviousDownload(){
        if(!downloaded_file.exists())
            try{ downloaded_file.createNewFile();} catch (IOException e) {e.printStackTrace();}
        else{
            String []read_file_woo;
            try{
                FileInputStream inputStream = new FileInputStream(downloaded_file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    read_file_woo = line.split(" ");
                    for(int i = 0; i < read_file_woo.length; i++) // data is stored >> title author id Url Duration
                        isDownloaded[i] = Boolean.parseBoolean(read_file_woo[i]);
                }
                inputStream.close(); }catch(OutOfMemoryError om){om.printStackTrace();}catch(Exception ex){ex.printStackTrace();}
        }
    }
    @Override
    public void play() {
        if (selectedBook != null) {
            playingBook = selectedBook;
            int playingBookId = playingBook.getId();
            controlFragment.setNowPlaying(getString(R.string.now_playing, playingBook.getTitle()));
            if (serviceConnected) {
                System.out.println("00000000000000000000000000000000000000000000000000000000000" + isDownloaded[playingBookId-1]);
                if(isDownloaded[playingBookId-1]){
                    System.out.println("************************************************************************************* " + rememberedTime[playingBookId-1]);
                    File toBePlayed = new File(getFilesDir(), String.valueOf(playingBookId));
                    mediaControl.play(toBePlayed,rememberedTime[playingBookId-1]);
                }else {
                    mediaControl.play(selectedBook.getId()); //if the book isnt download play from the beginning
                }
            }

            // Make sure that the service doesn't stop
            // if the activity is destroyed while the book is playing
            startService(serviceIntent);
        }
    }


    @Override
    public void pause(int progress) {
        if (serviceConnected) {
            mediaControl.pause();
            rememberedTime[playingBook.getId()-1] = (int) (((progress-10) / 100f) * playingBook.getDuration()); //this saves the progress minus the 10 seconds
        }
    }

    @Override
    public void stop() {
        if (serviceConnected)
            rememberedTime[playingBook.getId()-1] = 0;
            mediaControl.stop();

        // If no book is playing, then it's fine to let
        // the service stop once the activity is destroyed
        stopService(serviceIntent);
    }

    @Override
    public void changePosition(int progress) {
        if (serviceConnected)
            mediaControl.seekTo((int) ((progress / 100f) * playingBook.getDuration()));
    }

    @Override
    public void updateRememberTime(int progress){
        rememberedTime[playingBook.getId()-1] = (int) (((progress-10) / 100f) * playingBook.getDuration());
    }

    @Override
    protected void onStop() {
        super.onStop();
        FileOutputStream fos;
        try {
            fos = openFileOutput(time_file_name, MODE_PRIVATE);
            String s = "";
            for(int i = 0; i < rememberedTime.length-1; i++){
                s += rememberedTime[i] + " ";
            }
            s += rememberedTime[rememberedTime.length-1]+ '\n';
            //System.out.println(s);
            fos.write(s.getBytes());
            fos.close();} catch (IOException e) {e.printStackTrace();}
        try {
            fos = openFileOutput(downloaded_file_name, MODE_PRIVATE);
            String s = "";
            for(int i = 0; i < isDownloaded.length-1; i++){
                s += isDownloaded[i] + " ";
            }
            s += isDownloaded[isDownloaded.length-1] + " " + '\n';
            //System.out.println(s);
            fos.write(s.getBytes());
            fos.close();} catch (IOException e) {e.printStackTrace();}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }
}