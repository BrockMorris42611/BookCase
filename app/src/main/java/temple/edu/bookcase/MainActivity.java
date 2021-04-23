package temple.edu.bookcase;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;

import edu.temple.audiobookplayer.AudiobookService;
import edu.temple.audiobookplayer.AudiobookService.BookProgress;
import edu.temple.audiobookplayer.AudiobookService.MediaControlBinder;

public class MainActivity extends AppCompatActivity implements BookListFragment.BookListFragmentInterface, BookDetailsFragment.BookDetailsFragmentInterface, ControlFragment.ControlFragmentInterface {

    BookList BookList_MainActivity = new BookList(new ArrayList<>());
    String restoreBookList = "RESTORE_LIST";
    String restorePointAsOfAction = "RESTORE_TIME_STAMP";
    String restoreBinaryAudioState = "RESTORE_PLAYING_OR_NOT";

    BookListFragment BLF;
    BookDetailsFragment BDF;
    ControlFragment BCF;
    FragmentManager fragmentManager = getSupportFragmentManager();

    BookProgress bookProgressMessage;
    MediaControlBinder MCbinder;
    boolean connected = false;
    int point_asof_action = 0;
    boolean isPlaying = false;
    boolean ready = false;

    ServiceConnection serviceConnection;

    boolean split; //what viewing mode are we in?

    int selectionOnResetForDetailFrag; //hold our selection in case of context switch
    String restoreSelection = "RESTORE_SELEC";


    int REQUEST_CODE = 1;

    String BookL_FragTag = "BLF"; String BookL_BackTag = "ListB";
    String BookD_FragTag = "BDF"; String BookD_BackTag = "DetailB";
    String BookC_FragTag = "BCF"; String BookC_BackTag = "ControlB";
    String LOG_TAG = ">>>>>>>>>>>>>>>>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BCF = ControlFragment.newInstance();
        fragmentManager.beginTransaction().add(R.id.controlFrame, BCF, BookC_FragTag).setReorderingAllowed(true).addToBackStack(BookC_BackTag).commit();


        split = findViewById(R.id.detail_frame)!=null; //true when we can find the view(meaning we are in portrait) or false when we cant find the view(landscape or large)
        Button start_Search = findViewById(R.id.lookUpButton);

        if(savedInstanceState==null) { //This is to verify that the fragment is added only once when the activity is created
            BLF = BookListFragment.newInstance(BookList_MainActivity);
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction
                    .add(R.id.main_frame,BLF, BookL_FragTag)
                    .setReorderingAllowed(true)
                    .addToBackStack(BookL_BackTag)
                    .commit();
        } else{ //if we are operating on a savedInstance that isnt null (i.e. context switch) then pop the back stack for the name ands see if exists
            ready = false;
            selectionOnResetForDetailFrag = savedInstanceState.getInt(restoreSelection,0);
            BookList_MainActivity = savedInstanceState.getParcelable(restoreBookList);

            BCF.updateSelection(BookList_MainActivity.getLibrary().get(selectionOnResetForDetailFrag));
            isPlaying = savedInstanceState.getBoolean(restoreBinaryAudioState);
            point_asof_action = savedInstanceState.getInt(restorePointAsOfAction);

            boolean pop = fragmentManager.popBackStackImmediate(BookD_BackTag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            if(pop){ //if the frament type did exist then keep it but now load it into the main_frame frameview

                int frame_based_on_split = R.id.main_frame;//when we are in portrait single view DEFAULT VALUE
                if(split) //if we arent in the single screen then move the new selection to the other frame
                    frame_based_on_split = R.id.detail_frame;

                BDF = BookDetailsFragment.newInstance(BookList_MainActivity.getLibrary().get(selectionOnResetForDetailFrag));
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction
                        .add(frame_based_on_split, BDF, BookD_FragTag)
                        .setReorderingAllowed(true)
                        .addToBackStack(BookD_BackTag)
                        .commit();
            }
        }

        start_Search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BookSearchActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
                if(split){ //THIS IS TO MAKE SURE THE NEW LIST APPEARS WITHOUT ANY OLD SELECTIONS SHOWING FOR POSSIBLE OUT OF BOUNDS ARRAY
                    fragmentManager.beginTransaction()
                            .remove(BDF).commit();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intentForBind = new Intent(MainActivity.this, AudiobookService.class);

        bindService(intentForBind, serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {

                MCbinder = (MediaControlBinder) binder;

                connected = true;
                if (connected) {
                    MCbinder.setProgressHandler(new Handler(new Handler.Callback() {
                        @Override
                        public boolean handleMessage(@NonNull Message msg) {
                            try {
                                bookProgressMessage = (BookProgress) msg.obj;
                                Log.d("Progress via Handler >", String.valueOf(bookProgressMessage.getProgress()));
                                BCF.updateRealTime(bookProgressMessage.getProgress());
                                point_asof_action = bookProgressMessage.getProgress();
                                return true;
                            } catch (Exception e) {
                                e.printStackTrace();
                                return false;
                            }
                        }
                    }));
                    Log.d(LOG_TAG, "onServiceConnected");
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                connected = false;
                Log.d(LOG_TAG, "onServiceDisconnected");
            }
        }, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connected = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putParcelable(restoreBookList, BookList_MainActivity); //save for the changing of activities so we can remember what to display
        outState.putInt(restoreSelection, selectionOnResetForDetailFrag);
        outState.putInt(restorePointAsOfAction, point_asof_action);
        outState.putBoolean(restoreBinaryAudioState, MCbinder.isPlaying());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void sendSelectionBack(int sel) {

        selectionOnResetForDetailFrag = sel;
        int frame_based_on_split = R.id.main_frame;//when we are in portrait single view DEFAULT VALUE
        if(split) //if we arent in the single screen then move the new selection to the other frame
            frame_based_on_split = R.id.detail_frame;

        BDF = BookDetailsFragment.newInstance(BookList_MainActivity.getLibrary().get(sel));
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction
                .replace(frame_based_on_split, BDF, BookD_FragTag)
                .setReorderingAllowed(true)
                .addToBackStack(BookD_BackTag)
                .commit();
        BCF.updateSelection(BookList_MainActivity.getLibrary().get(sel));
        //MCbinder.stop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        System.out.println("AFKJLKGFHDL:KGHDBG:FKJDGBHF:KJDSGBF");;
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == 12 && data != null) { //check we are getting the right info back to the main activity form the secondary search
            Bundle b = data.getExtras();
            if (b.getParcelable("BOOK_LIST") != null) { //make sure the key exists via key val pair
                BookList_MainActivity = data.getParcelableExtra("BOOK_LIST");
                BLF= BookListFragment.newInstance(BookList_MainActivity);

                fragmentManager.beginTransaction() //after we get the new selection make the fragment and replace the current one main_frame
                        .replace(R.id.main_frame, BLF, BookL_FragTag)
                        .setReorderingAllowed(true)
                        .addToBackStack(BookL_BackTag)
                        .commit();
            } else {
                System.out.println(data.toString());
            }
        }
    }
    @Override
    public void display(Book b) {}

    @Override
    public void playBookOnClick(int start_point) {
        //isPlaying = true;
        point_asof_action = start_point;
        MCbinder.play(BookList_MainActivity.getLibrary().get(selectionOnResetForDetailFrag).getId(), point_asof_action);
    }

    @Override
    public void pauseBookOnClick(int pause_point) {
        if(MCbinder.isPlaying()) {
            MCbinder.pause();
            //isPlaying = false;
        }else {
            MCbinder.play(BookList_MainActivity.getLibrary().get(selectionOnResetForDetailFrag).getId(), point_asof_action);
            //isPlaying = true;
        }
    }

    @Override
    public void seekToBookOnClick(int goto_point){
        MCbinder.seekTo(goto_point);
    }
    @Override
    public void stopBookOnClick() {
        MCbinder.stop();
        point_asof_action = 0;
        isPlaying = false;
    }
}