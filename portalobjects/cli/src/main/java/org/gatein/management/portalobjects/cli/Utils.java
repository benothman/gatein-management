/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.gatein.management.portalobjects.cli;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.xml.DOMConfigurator;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

/**
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @version $Revision$
 */
public class Utils
{
   private Utils()
   {
   }

   public static void addPropertiesAsArgs(Class<?> clazz, Properties properties, List<String> argList, String[] fieldNames, String[] flags) throws Exception
   {
      for (String fieldName : fieldNames)
      {
         Field field = clazz.getDeclaredField(fieldName);
         Option option = field.getAnnotation(Option.class);
         if (option != null)
         {
            if (!containsArgument(argList, option))
            {
               String key = (option.name().startsWith("--")) ? option.name().substring(2) : option.name();
               String value = properties.getProperty("arg." + key);
               if (value != null)
               {
                  if (!containsFlag(key, flags))
                  {
                     argList.add(0, option.name());
                     argList.add(1, value);
                  }
                  else
                  {
                     if (Boolean.valueOf(value))
                     {
                        argList.add(0, option.name());
                     }
                  }
               }
            }
         }
      }
   }

   public static boolean containsArgument(List<String> argList, Option option)
   {
      List<String> options = new ArrayList<String>();
      options.add(option.name());
      options.addAll(Arrays.asList(option.aliases()));
      for (String s : options)
      {
         if (argList.contains(s)) return true;
      }
      return false;
   }

   public static boolean containsFlag(String optionName, String[] flags)
   {
      if (flags == null) return false;
      for (String flag : flags)
      {
         if (flag.equals(optionName)) return true;
      }

      return false;
   }

   public static String trimToNull(String value)
   {
      if (value == null) return null;

      value = value.trim();
      if ("".equals(value)) return null;

      return value;
   }

   public static String getUserInput(String text, int indentLevel)
   {
      indent(indentLevel);
      System.out.printf("%s: ", text);
      Scanner scanner = new Scanner(System.in);
      return scanner.nextLine();
   }

   public static void indent(int level)
   {
      for (int i = 0; i < level; i++)
      {
         System.out.print("   ");
      }
   }

   public static void initializeLogging(File log4jConfigFile, String logLevel, File logDir, String fileName, String program)
   {
      if (log4jConfigFile == null)
      {
         System.setProperty("gtn.log.dir", logDir.getAbsolutePath());
         String logFile = fileName + "-" + program + ".log";
         System.setProperty("gtn.log.file", logFile);
         System.out.println("Logging to " + logDir.getAbsolutePath() + "/" + logFile);
         DOMConfigurator.configure(Main.class.getResource("/log/log4j.xml"));
      }
      else
      {
         try
         {
            DOMConfigurator.configure(log4jConfigFile.toURI().toURL());
         }
         catch (MalformedURLException e)
         {
            System.err.println("Could not configure log4j config file " + log4jConfigFile + ".");
            e.printStackTrace(System.err);
            System.exit(1);
         }
         System.out.println("Using log4j configuration file " + log4jConfigFile + " for logging.");
      }
      if (logLevel != null)
      {
         LogManager.getRootLogger().setLevel(Level.toLevel(logLevel));
      }
   }
}
