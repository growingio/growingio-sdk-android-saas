package com.growingio.android.sdk.models;

/**
 * author CliffLeopard
 * time   2017/9/11:下午1:59
 * email  gaoguanling@growingio.com
 */

public class LocationBean {
    private double latitude;
    private double longitude;
    private String countryCode;
    private String country;
    private String region;
    private String city;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }


    public String toJson()
    {
        String json = null;
        return  json;

    }

}
