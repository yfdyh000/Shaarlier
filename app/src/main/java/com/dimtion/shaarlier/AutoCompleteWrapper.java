package com.dimtion.shaarlier;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;

import java.io.IOException;
import java.util.List;

/**
 * Created by dimtion on 21/02/2015. 
 * Inspired from : http://stackoverflow.com/a/5051180
 * and : http://www.claytical.com/blog/android-dynamic-autocompletion-using-google-places-api
 */
class AutoCompleteWrapper {

    private final MultiAutoCompleteTextView a_textView;
    private final Context a_context;
    private ArrayAdapter<Tag> adapter;

    public AutoCompleteWrapper(final MultiAutoCompleteTextView textView, Context context) {
        this.a_textView = textView;
        this.a_context = context;

        this.a_textView.setTokenizer(new SpaceTokenizer());

        this.adapter = new ArrayAdapter<>(a_context, R.layout.tags_list);
        this.a_textView.setAdapter(this.adapter);
        this.a_textView.setThreshold(1);
        updateTagsView();

        AutoCompleteRetriever task = new AutoCompleteRetriever();
        task.execute();
    }

    private void updateTagsView() {
        TagsSource tagsSource = new TagsSource(a_context);
        tagsSource.rOpen();
        List<Tag> tagList = tagsSource.getAllTags();

        this.adapter.clear();
        this.adapter.addAll(tagList);
        this.adapter.notifyDataSetChanged();

        this.a_textView.setAdapter(this.adapter);

        tagsSource.close();
    }

    private class AutoCompleteRetriever extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... foo) {
            AccountsSource accountsSource = new AccountsSource(a_context);
            accountsSource.rOpen();
            List<ShaarliAccount> accounts = accountsSource.getAllAccounts();

            TagsSource tagsSource = new TagsSource(a_context);
            tagsSource.wOpen();
            /* For the moment we keep all the tags, if later somebody wants to have the tags
            ** separated for each accounts, we will see
            */
            for (ShaarliAccount account : accounts) {
                // Download tags :
                NetworkManager manager = new NetworkManager(
                        account.getUrlShaarli(),
                        account.getUsername(),
                        account.getPassword());
                try {
                    manager.retrieveLoginToken();
                    manager.login();
                    String[] awesompleteTags = manager.retrieveTagsFromAwesomplete();
                    String[] wsTags = manager.retrieveTagsFromWs();  // Keep for compatibility
                    for (String tagValue : awesompleteTags) {
                        tagsSource.createTag(account, tagValue.trim());
                    }
                    for (String tagValue : wsTags) {
                        tagsSource.createTag(account, tagValue);
                    }
                } catch (IOException e) {
                    Log.e("ERROR", e.toString());
                }
            }
            tagsSource.close();
            accountsSource.close();
            return 0;
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(Integer r) {
            updateTagsView();
        }
    }
}
