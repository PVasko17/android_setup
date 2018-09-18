package com.devforfun.app.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.devforfun.app.R;
import com.devforfun.app.callbacks.IListElementClickCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ListsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private JSONArray mDataset;
    private Context mContext;
    private IListElementClickCallback elementInteraction;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ListElementViewHolder extends RecyclerView.ViewHolder {

        public ListElementViewHolder(View cardView) {
            super(cardView);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ListsAdapter(JSONArray dataset, Context context, IListElementClickCallback elementInteraction) {
        this.mDataset = dataset;
        this.mContext = context;
        this.elementInteraction = elementInteraction;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View rootView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.element_list, parent, false);
        return new ListElementViewHolder(rootView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        final JSONObject listItem = getItem(viewHolder.getAdapterPosition());
        final ListElementViewHolder holder = (ListElementViewHolder) viewHolder;

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                elementInteraction.onItemClicked(v, holder.getAdapterPosition());
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.length();
    }

    private JSONObject getItem(int position) {
        try {
            return mDataset.getJSONObject(position);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
}