package com.github.warren_bank.filterablerecyclerview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FilterableAdapter extends RecyclerView.Adapter<FilterableViewHolder> implements Filterable {
    final private int row_layout_id;
    final private List<FilterableListItem> unfilteredList;
    final private ArrayList<FilterableListItem> filteredList;
    final private FilterableListItemOnClickListener listener;
    final private Class filterableViewHolderClass;
    final private Class parentClass;
    final private Object parentInstance;
    final private Filter searchFilter;

    public FilterableAdapter(
        int row_layout_id,
        List<FilterableListItem> unfilteredList,
        FilterableListItemOnClickListener listener,
        Class filterableViewHolderClass
    ) {
        this(
            row_layout_id,
            unfilteredList,
            listener,
            filterableViewHolderClass,
            (Class) null,
            (Object) null
        );
    }

    public FilterableAdapter(
        int row_layout_id,
        List<FilterableListItem> unfilteredList,
        FilterableListItemOnClickListener listener,
        Class filterableViewHolderClass,
        Class parentClass,
        Object parentInstance
    ) {
        this.row_layout_id             = row_layout_id;
        this.unfilteredList            = unfilteredList;
        this.filteredList              = new ArrayList<FilterableListItem>();
        this.listener                  = listener;
        this.filterableViewHolderClass = filterableViewHolderClass;
        this.parentClass               = parentClass;
        this.parentInstance            = parentInstance;
        this.searchFilter              = createSearchFilter();

        resetFilteredList();
    }

    private void resetFilteredList() {
        filteredList.clear();
        filteredList.addAll(
            unfilteredList
        );
    }

    @Override
    public FilterableViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        try {
            View view = LayoutInflater.from(parent.getContext()).inflate(row_layout_id, parent, false);
            Class[] cArg;
            FilterableViewHolder holder;

            if ((parentClass == null) || (parentInstance == null)) {
                cArg     = new Class[3];
                cArg[0]  = View.class;
                cArg[1]  = List.class;
                cArg[2]  = FilterableListItemOnClickListener.class;

                holder = (FilterableViewHolder) filterableViewHolderClass.getDeclaredConstructor(cArg).newInstance(
                    view,
                    filteredList,
                    listener
                );
            }
            else {
                cArg     = new Class[4];
                cArg[0]  = parentClass;
                cArg[1]  = View.class;
                cArg[2]  = List.class;
                cArg[3]  = FilterableListItemOnClickListener.class;

                holder = (FilterableViewHolder) filterableViewHolderClass.getDeclaredConstructor(cArg).newInstance(
                    parentInstance,
                    view,
                    filteredList,
                    listener
                );
            }

            return holder;
        }
        catch(Exception e) {
            return null;
        }
    }

    @Override
    public void onBindViewHolder(FilterableViewHolder holder, final int position) {
        try {
            final FilterableListItem item = filteredList.get(position);

            holder.onUpdate(item);
        }
        catch(Exception e) {}
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    @Override
    public Filter getFilter() {
        return searchFilter;
    }

    private Filter createSearchFilter() {
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    resetFilteredList();
                } else {
                    filteredList.clear();
                    for (FilterableListItem item : unfilteredList) {
                        String filterableValue = item.getFilterableValue();

                        if (filterableValue.toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(item);
                        }
                    }
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredList;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                notifyDataSetChanged();
            }
        };
    }

    // To be called when "unfilteredList" is updated outside of this adapter. (note: this Object is stored by reference.)
    // Under normal conditions, "notifyDataXXX" methods would be called on an adapter.
    // However, these methods are "final".. so I couldn't extend them.
    // This "refresh" method results in:
    //   * data being copied (and optionally filtered) into "filteredList" from "unfilteredList"
    //   * "notifyDataSetChanged" being called on the adapter after "filteredList" has been updated
    public void refresh() {
        searchFilter.filter(searchFilter.constraint);
    }
}
