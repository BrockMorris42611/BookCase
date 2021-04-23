package temple.edu.bookcase;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class BookListViewAdapter extends BaseAdapter{

    Context context;
    ArrayList<Book> bookArrayList;

    public BookListViewAdapter(Context context, ArrayList<Book> bookArrayList){
        this.context = context;
        this.bookArrayList = bookArrayList;
    }
    @Override
    public int getCount() {
        return bookArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return bookArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout LinearLayout;
        TextView titleTextView;
        TextView authorTextView;
        if(convertView == null) {
            LinearLayout = new LinearLayout(context);
            LinearLayout.setOrientation(android.widget.LinearLayout.VERTICAL); //display author UNDER book title
            titleTextView = new TextView(context);
            titleTextView.setTextSize(30);
            authorTextView = new TextView(context);
            authorTextView.setTextSize(15);
            LinearLayout.addView(titleTextView);
            LinearLayout.addView(authorTextView);
        }else{
            LinearLayout = (LinearLayout) convertView;
            titleTextView = (TextView) LinearLayout.getChildAt(0);
            authorTextView = (TextView) LinearLayout.getChildAt(1);
        }
        titleTextView.setText(bookArrayList.get(position).getTitle());
        authorTextView.setText(bookArrayList.get(position).getAuthor());


        return LinearLayout;
    }
}
