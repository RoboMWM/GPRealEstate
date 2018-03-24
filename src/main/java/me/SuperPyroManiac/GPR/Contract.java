package me.SuperPyroManiac.GPR;

import java.sql.Timestamp;
import java.util.Date;

public class Contract
{
    public String Owner;
    public String Landlord;
    public Double Price;
    public Timestamp NextRenewal;
    public Timestamp LastRenewal;

    public Contract(String landlord, String owner, Date renewal)
    {
        this.Owner = owner;
        this.Landlord = landlord;

        Date date = new Date();

        this.LastRenewal = new Timestamp(date.getTime());
        this.NextRenewal = new Timestamp(renewal.getTime());
    }
}
