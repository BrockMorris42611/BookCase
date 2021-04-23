package temple.edu.bookcase;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.Objects;

import temple.edu.bookcase.Book;
import temple.edu.bookcase.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ControlFragment extends Fragment {

    public Button playb, pauseb, stopb;
    public SeekBar bookProgressSeekBar;
    public TextView nowPlayingTV;

    ControlFragmentInterface tester;

    int bookProgress, maxDur = 0;
    Book book;
    boolean fromInitOfBook = false;
    boolean created = true;

    public ControlFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            tester = (ControlFragment.ControlFragmentInterface) context; //make sure we are operating on the context of a correct interface activity
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement BookListFragmentInterface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        tester = null; //null the allocated tester from onAttach()
    }

    // TODO: Rename and change types and number of parameters
    public static ControlFragment newInstance() {
        ControlFragment fragment = new ControlFragment();
        Bundle args = new Bundle();
        //args.putParcelable("BookInfo", BookListF);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            bookProgress = savedInstanceState.getInt("z");
            book = savedInstanceState.getParcelable("zz");
        }
        /*if (getArguments() != null) {
        }*/
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_control, container, false);
        playb = v.findViewById(R.id.play_button);
        pauseb = v.findViewById(R.id.pause_button);
        stopb = v.findViewById(R.id.stop_button);
        nowPlayingTV = v.findViewById(R.id.nowPlayingTV);
        bookProgressSeekBar = v.findViewById(R.id.audioSeekBar);
        v.findViewById(R.id.controlCL).setBackgroundColor(Color.parseColor("#68a0b0"));
        bookProgressSeekBar.setBackgroundColor(Color.parseColor("#6638e2"));

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        playb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tester.playBookOnClick(bookProgress);
            }
        });
        pauseb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tester.pauseBookOnClick(bookProgress);
            }
        });
        stopb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bookProgress = 0;
                tester.stopBookOnClick();
            }
        });
        bookProgressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(!fromInitOfBook) {
                    if (fromUser)
                        tester.seekToBookOnClick(progress);
                    else
                        bookProgressSeekBar.setProgress(progress);
                }else{
                    fromInitOfBook = false;
                    bookProgressSeekBar.setMax(maxDur);
                    bookProgressSeekBar.setProgress(bookProgress);
                    String s = "Now Playing: " + book.getTitle() + " by " + book.getAuthor();
                    nowPlayingTV.setText(s);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar){}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar){
                onProgressChanged(seekBar, seekBar.getProgress(), true);
            }});
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if(savedInstanceState != null){
            System.out.println("**********************************************************************************");
            bookProgressSeekBar.setProgress(bookProgress);
            bookProgressSeekBar.setMax(book.getDuration());
            updateRealTime(bookProgress);
            String s = "Now Playing: " + book.getTitle() + " by " + book.getAuthor();
            nowPlayingTV.setText(s);
            created = true;
        }
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("z", bookProgress);
        outState.putParcelable("zz", book);
    }

    public void updateRealTime(int bookProgress){
        this.bookProgress = bookProgress;
        bookProgressSeekBar.setProgress(this.bookProgress);
    }
    public void updateSelection(Book book){
        this.book = book;
        maxDur = book.getDuration();
        fromInitOfBook = true;
        bookProgress = 0;
    }
    public interface ControlFragmentInterface{
        void playBookOnClick(int start_point);
        void pauseBookOnClick(int pause_point);
        void seekToBookOnClick(int goto_point);
        void stopBookOnClick();
    }
}