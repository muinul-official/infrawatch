package com.example.infrawatchmapdev;

import com.google.firebase.Timestamp;
import java.util.List;

public class RepairReport {

    public String damage_Category;
    public String specific_Issue_Type;
    public String description;
    public String location;
    public List<String> photoVideoUrls;
    public Timestamp timestamp;
    public String userId;
    public String status;


    public RepairReport() {}
}

