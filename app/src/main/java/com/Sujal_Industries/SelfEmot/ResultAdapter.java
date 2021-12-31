package com.Sujal_Industries.SelfEmot;

import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ResultViewHolder> {

    private final ArrayList<Result> dataList;

    public ResultAdapter(ArrayList<Result> dataList)
    {
        this.dataList = dataList;
    }
    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
        View view = layoutInflater.inflate(R.layout.card, viewGroup, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder resultViewHolder, int i) {
        resultViewHolder.name123.setText(dataList.get(i).getName());
        resultViewHolder.confidence123.setText(dataList.get(i).getConfidence());
        StringBuilder sb=new StringBuilder(dataList.get(i).getConfidence());
        sb.deleteCharAt(sb.length()-1);
        String conf=sb.toString();
        float confidence=Float.parseFloat(conf);
        if(confidence>=70)
        {
            resultViewHolder.confidence123.setTextColor(Color.rgb(34,139,34));
        }
        else
        {
            resultViewHolder.confidence123.setTextColor(Color.RED);
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder{

        TextView name123,confidence123;

        public ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            name123 = itemView.findViewById(R.id.name);
            confidence123 = itemView.findViewById(R.id.confidence);
        }
    }
}
