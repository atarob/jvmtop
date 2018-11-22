/**
 * jvmtop - java monitoring for the command-line
 *
 * Copyright (C) 2013 by Patric Rufflar. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.jvmtop;

import java.lang.Thread.State;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map.Entry;

import com.jvmtop.monitor.VMInfo;
import com.jvmtop.openjdk.tools.LocalVirtualMachine;
/*
 * @author ata
 *
 */
public class JvmTop
{
    private static String[] filteredPackages = new String[] {
                "org.eclipse.", "org.apache.", "java.", "sun.", "com.sun.", "javax.",
                "oracle.", "com.trilead.", "org.junit.", "org.mockito.",
                "org.hibernate.", "com.ibm.", "com.caucho."
            };
  
    private static String[] filteredClass = new String[] { "sun.nio.ch.EPollArrayWrapper" };
    private static String[] filteredMethod = new String[] { "epollWait" };
  
    public static void main(String[] args) throws Exception
    {
        final int pid = Integer.valueOf(args[0]);
        final long delayMillis = Long.valueOf(args[1]);
        final long totalMillis = Long.valueOf(args[2]);
        LocalVirtualMachine localVirtualMachine = LocalVirtualMachine.getLocalVirtualMachine(pid);
        VMInfo vmInfo = VMInfo.processNewVM(localVirtualMachine, pid);  
        ThreadMXBean bean = vmInfo.getThreadMXBean();
        HashMap<String, Long> hits = new HashMap<String, Long>();      
        final long endTime = System.currentTimeMillis() + totalMillis;
        long totalSamples = 0;
        while(System.currentTimeMillis() < endTime) {
            for (ThreadInfo ti : bean.dumpAllThreads(false, false))
            {
                if (ti.getStackTrace().length > 0 && ti.getThreadState() == State.RUNNABLE) {
                    StackTraceElement child = null;
                    for (StackTraceElement parent : ti.getStackTrace()) {
                        if(isFiltered(parent)) {
                            break;
                        }
                        if(child == null) { 
                            child = parent;
                            continue; 
                        }
                        String key = parent.getClassName() + "." + parent.getMethodName() + "():" + parent.getLineNumber() + " ===> "+ child.getClassName() + "." + child.getMethodName();
                        if(hits.containsKey(key)) {
                            hits.put(key, hits.get(key) + 1);
                        } else {
                            hits.put(key, 1L);
                        }
                        child = parent;
                    }
                }
            }
            totalSamples++;         
            if(delayMillis > 0) {
              Thread.sleep(delayMillis);
            }
        }
        for(Entry<String, Long> e : hits.entrySet()) {
            System.out.println(e.getValue()+":"+e.getKey());
        }
        System.out.println(String.format("Total Samples: %d", totalSamples));
    }

    public JvmTop()
    {
    }
  
    static private boolean isFiltered(StackTraceElement se) {
      for (String filteredPackage : filteredPackages) {
          if (se.getClassName().startsWith(filteredPackage)) {
              return true;
          }
      }
      for(int i=0; i < filteredClass.length; i++) {
          if (se.getClassName().equals(filteredClass[i]) && se.getMethodName().equals(filteredMethod[i])) {
              return true;
          }
      }
      return false;
    }

}
