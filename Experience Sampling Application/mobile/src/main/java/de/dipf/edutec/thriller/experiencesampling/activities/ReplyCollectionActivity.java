package de.dipf.edutec.thriller.experiencesampling.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.messageservice.MessagesSingleton;
import de.dipf.edutec.thriller.experiencesampling.foreground.MyRecyclerViewAdapter;
import de.dipf.edutec.thriller.experiencesampling.messagestruct.MyMessage;

public class ReplyCollectionActivity extends AppCompatActivity implements MyRecyclerViewAdapter.ItemClickListener {

    //GUI
    ExtendedFloatingActionButton floatingActionButton;

    public MessagesSingleton messagesSingleton;
    public MyRecyclerViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reply_collection);
        messagesSingleton = MessagesSingleton.getInstance();

        floatingActionButton = findViewById(R.id.bt_replyColl_refresh);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshLayout();
            }
        });

        messagesSingleton.registerListener(new MessagesSingleton.Listener() {
            @Override
            public String onStateChange(String uuid) {
                refreshLayout();
                return null;
            }
        });


        refreshLayout();
    }



    public void refreshLayout(){
        List<String> questionList = new ArrayList<String>();
        List<String> answerList = new ArrayList<String>();

        for(MyMessage msg : messagesSingleton.messagesReceived){
            questionList.add(msg.getQuestion());
            answerList.add(msg.getUserAnswer());
        }

        RecyclerView recyclerView = findViewById(R.id.rv_reply_collection);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyRecyclerViewAdapter(this, questionList, answerList);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(View view, int position) {

    }
}
