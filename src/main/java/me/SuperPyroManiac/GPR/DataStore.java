package me.SuperPyroManiac.GPR;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginDescriptionFile;

public class DataStore
{
    GPRealEstate plugin;
    public PluginDescriptionFile pdf;
    public final String pluginDirPath = "plugins" + File.separator + "GPRealEstate" + File.separator;
    public final String configFilePath = this.pluginDirPath + "config.yml";
    public final String logFilePath = this.pluginDirPath + "GPRealEstate.log";
    public final String chatPrefix = "[" + ChatColor.GOLD + "GPRealEstate" + ChatColor.WHITE + "] ";
    public String cfgSignShort;
    public String cfgSignLong;
    public List<String> cfgRentKeywords;
    public List<String> cfgSellKeywords;
    public String cfgReplaceRent;
    public String cfgReplaceSell;
    public int cfgReplaceValue;
    public boolean cfgEnableLeasing;
    public boolean cfgIgnoreClaimSize;
    public boolean cfgAllowSellingParentAC;
    public boolean cfgTransferClaimBlocks;

    public DataStore(GPRealEstate plugin)
    {
        this.plugin = plugin;
        this.pdf = this.plugin.getDescription();
    }

    public List<String> stringToList(String input)
    {
        //String[] array = { input.matches("([;+])") ? input.split(";") : input }; //Decompiled code. Doesn't seem to be valid...
        return Arrays.asList(input.split(";"));
    }

    public String listToString(List<String> input)
    {
        String string = "";
        int count = 1;
        Object[] arrayOfObject;
        int j = (arrayOfObject = input.toArray()).length;
        for (int i = 0; i < j; i++)
        {
            Object str = arrayOfObject[i];
            if (count != 1)
            {
                count++;
                string = string + ";";
            }
            string = string + str.toString();
        }
        return string;
    }
}
