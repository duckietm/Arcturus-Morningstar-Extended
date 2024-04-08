package com.skeletor.plugin.javascript.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtility {
  public static String getYouTubeId(String youTubeUrl) {
    String pattern = "(?<=youtu.be/|watch\\?v=|/videos/|embed\\/)[^#\\&\\?]*";
    Pattern compiledPattern = Pattern.compile(pattern);
    Matcher matcher = compiledPattern.matcher(youTubeUrl);
    if (matcher.find())
      return matcher.group(); 
    return "";
  }
}
