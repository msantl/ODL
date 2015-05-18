/* TOTEM-v3.2 June 18 2008*/

/*
 * ===========================================================
 * TOTEM : A TOolbox for Traffic Engineering Methods
 * ===========================================================
 *
 * (C) Copyright 2004-2006, by Research Unit in Networking RUN, University of Liege. All Rights Reserved.
 *
 * Project Info:  http://totem.run.montefiore.ulg.ac.be
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License version 2.0 as published by the Free Software Foundation;
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc.
 * in the United States and other countries.]
*/

package com.example.samcra;

/*
 * Changes:
 * --------
 * 05-Mar.-2007: Suppress library loading (GMO)
 */

/**
 * This class implements the JNI interface for XAMCRA : SAMCRA, TAMCRA, ... (TUDelft).
 * <p/>
 * <p>Creation date : 28 nov. 2005 11:23:05
 *
 * @author Simon Balon (balon@run.montefiore.ulg.ac.be)
 */
public class JNIXAMCRA {
    // Print the XAMCRA DB in the C code
    public native static void jniprintXamcraDB();

    /**
     * Initializes XAMCRA
     * High Level configuration, database filling,...
     * @param xamcraVersion : specify if we want to use SAMCRA (type = 0) or TAMCRA (type = 1) or ...
     */
    public native static void jniinitXamcra(int numNodes, int numLinks, int nbMetrics, int xamcraVersion);

    /**
     * Kills XAMCRA
     * Unload XAMCRA from memory
     */
    public native static void jnikillXamcra();

    /**
     * Adds Node in XAMCRA database
     * @param nodeId
     */
    public native static void jniaddNode(int nodeId);

    /**
     * Adds Link in XAMCRA database
     * @param srcId
     * @param dstId
     * @param cap (available) capacity of the link
     */
    public native static void jniaddLink(int srcId, int dstId, double cap, double[] qosMetric);

    /**
     * Adds an already computed primary LSP to XAMCRA database
     * @param path the path as a list of NODE ids
     * @param reservation requested bandwidth
     */
    public native static void  jniaddPath(int[] path, double reservation);

    /**
     * Remove an already computed primary LSP to XAMCRA database
     * @param path the path as a list of NODE ids
     * @param reservation requested bandwidth
     */
    public native static void  jniremovePath(int[] path, double reservation);

    /**
     * Computes a primary LSP with XAMCRA
     * @param src
     * @param dst
     * @param bandwidth requested bandwidth
     * @param ADDLSP add the LSP to the database
     * @return
     */
    public native static int[] jnicomputePath(int src, int dst, double bandwidth, double[] qosConstraint, int ADDLSP);
}
