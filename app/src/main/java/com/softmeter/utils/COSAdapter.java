package com.softmeter.utils;

import com.example.skhalid.softmetersimulation.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sKhalid on 4/6/2015.
 */
public class COSAdapter {
    private List<COS> cosValues;

    public COSAdapter(String packetBody){
        cosValues = new ArrayList<COS>();
        String [] packetBodyArray = packetBody.split("\\" + Constants.ROWSEPARATOR);
        for (int i=1; i<packetBodyArray.length; i++){
            cosValues.add(new COS(packetBodyArray[i]));
        }
    }

    public int size(){
        if(cosValues != null)
        return cosValues.size();
        else
            return 0;
    }

    public List<COS> values(){
            return cosValues;
    }
}
