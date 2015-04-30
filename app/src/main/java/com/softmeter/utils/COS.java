package com.softmeter.utils;

import com.example.skhalid.softmetersimulation.Constants;

/**
 * Created by sKhalid on 4/6/2015.
 */
public class COS {
    private String _ClassOfServiceID;
    private String _APF;
    private String _PUM;
    private String _PUT;
    private String _ADU;
    private String _ADC;
    private String _ATU;
    private String _ATC;
    private String _APC;
    private String _CPC;
    private String _DefaultClassOfService;

    public COS(String body){
        String [] bodyArray = body.split("\\" + Constants.COLSEPARATOR);
        if(bodyArray.length == 11) {
            _ClassOfServiceID = bodyArray[0];
            _APF = bodyArray[1];
            _PUM = bodyArray[2];
            _PUT = bodyArray[3];
            _ADU = bodyArray[4];
            _ADC = bodyArray[5];
            _ATU = bodyArray[6];
            _ATC = bodyArray[7];
            _APC = bodyArray[8];
            _CPC = bodyArray[9];
            _DefaultClassOfService = bodyArray[10];
        }

    }

    public String get_ClassOfServiceID() {
        return _ClassOfServiceID;
    }

    public void set_ClassOfServiceID(String _ClassOfServiceID) {
        this._ClassOfServiceID = _ClassOfServiceID;
    }

    public String get_APF() {
        return _APF;
    }

    public void set_APF(String _APF) {
        this._APF = _APF;
    }

    public String get_PUM() {
        return _PUM;
    }

    public void set_PUM(String _PUM) {
        this._PUM = _PUM;
    }

    public String get_PUT() {
        return _PUT;
    }

    public void set_PUT(String _PUT) {
        this._PUT = _PUT;
    }

    public String get_ADU() {
        return _ADU;
    }

    public void set_ADU(String _ADU) {
        this._ADU = _ADU;
    }

    public String get_ADC() {
        return _ADC;
    }

    public void set_ADC(String _ADC) {
        this._ADC = _ADC;
    }

    public String get_ATU() {
        return _ATU;
    }

    public void set_ATU(String _ATU) {
        this._ATU = _ATU;
    }

    public String get_ATC() {
        return _ATC;
    }

    public void set_ATC(String _ATC) {
        this._ATC = _ATC;
    }

    public String get_APC() {
        return _APC;
    }

    public void set_APC(String _APC) {
        this._APC = _APC;
    }

    public String get_CPC() {
        return _CPC;
    }

    public void set_CPC(String _CPC) {
        this._CPC = _CPC;
    }

    public String get_DefaultClassOfService() {
        return _DefaultClassOfService;
    }

    public void set_DefaultClassOfService(String _DefaultClassOfService) {
        this._DefaultClassOfService = _DefaultClassOfService;
    }
}
