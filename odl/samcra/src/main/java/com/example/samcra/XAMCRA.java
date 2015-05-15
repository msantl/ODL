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

package nl.tudelft.repository.XAMCRA;

import org.apache.log4j.Logger;
import be.ac.ulg.montefiore.run.totem.repository.model.*;
import be.ac.ulg.montefiore.run.totem.repository.model.exception.*;
import be.ac.ulg.montefiore.run.totem.domain.model.*;
import be.ac.ulg.montefiore.run.totem.domain.model.impl.PathImpl;
import be.ac.ulg.montefiore.run.totem.domain.model.impl.LspImpl;
import be.ac.ulg.montefiore.run.totem.domain.facade.InterDomainManager;
import be.ac.ulg.montefiore.run.totem.domain.exception.InvalidDomainException;
import be.ac.ulg.montefiore.run.totem.domain.exception.LspAlreadyExistException;
import be.ac.ulg.montefiore.run.totem.domain.exception.NodeNotFoundException;
import be.ac.ulg.montefiore.run.totem.util.IdGenerator;
import be.ac.ulg.montefiore.run.totem.util.ParameterDescriptor;

import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/*
 * Changes:
 * --------
 *  - 08-Dec-2005: Implement new getRunningParameters() from the TotemAlgorithm interface. + minor bugfix (GMO)
 *  - 12-Dec-2005: Simplify metrics handling (GMO)
 *  - 26-Jan-2006: remove TAMCRA from the possible version of the algo (GMO).
 *  - 23-Aug-2006: Invalidate database if backups are used in conjonction with BW Sharing, better error handling. (GMO)
 *  - 05-Mar-2007: Suppress library loading (GMO)
 *  - 13-Aug-2007: Start() method now throws exception in case of error (GMO)
 *  - 17-Sep-2007: set DB valid when starting/stopping the algorithm (GMO)
 */

// TODO verify that the QoS constraints are good... (available bandwidth, ...)
/**
 * This class implements the integration of XAMCRA (TUDelft).
 *
 * It is mandatory to specify the QoS constraints used when starting the algorithm.
 * By default, only one constraint is used : the bandwidth.
 * It is possible to use up to three additionnal constraints : the delai, metric and TE metric.
 * The boolean parameters useMetric, useTEMetric, useDelay are used to specify which QoS constraints are in use.
 *
 * When routing an LSP, the delay constraint must be given in an additionnal parameter called "DelayConstraint",
 * the metric constraint must be given in an additionnal parameter called "MetricConstraint" and the TE metric
 * constraint must be given in an additionnal parameter called "TEMetricConstraint". If not specified, these
 * values are set to Double.MAX_VALUE so that this means no constraint...
 *
 * <p/>
 * <p>Creation date : 28 nov. 2005 13:47:24
 *
 * @author Simon Balon (balon@run.montefiore.ulg.ac.be)
 */
public class XAMCRA implements LSPPrimaryRouting, DomainSyncAlgorithm {
    private static Logger logger = Logger.getLogger(XAMCRA.class);

    private static final ArrayList<ParameterDescriptor> params = new ArrayList<ParameterDescriptor>();
    private static final ArrayList<ParameterDescriptor> routingParams = new ArrayList<ParameterDescriptor>();
    static {
        try {
            params.add(new ParameterDescriptor("ASID", "Domain ASID (leave blank for default).", Integer.class, null));
            params.add(new ParameterDescriptor("version", "Version to use.", String.class, "SAMCRA", new String[] {"SAMCRA"}));
            params.add(new ParameterDescriptor("useDelay", "Use delay QoS constraint", Boolean.class, new Boolean(false)));
            params.add(new ParameterDescriptor("useMetric", "Use metric QoS constraint", Boolean.class, new Boolean(false)));
            params.add(new ParameterDescriptor("useTEMetric", "Use TEMetric QoS constraint", Boolean.class, new Boolean(false)));
        } catch (AlgorithmParameterException e) {
            e.printStackTrace();
        }
        try {
            routingParams.add(new ParameterDescriptor("addLSP", "Tell whether computed LSP should be directly added to local algorithm-specific DB.", Boolean.class, new Boolean(false)));
            routingParams.add(new ParameterDescriptor("DelayConstraint", "Delay constraint value.", Double.class, new Double(0.0)));
            routingParams.add(new ParameterDescriptor("MetricConstraint", "Metric constraint value.", Double.class, new Double(0.0)));
            routingParams.add(new ParameterDescriptor("TEMetricConstraint", "TE Metric constraint value.", Double.class, new Double(0.0)));
        } catch (AlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    private static DomainConvertor convertor = null;
    private static Domain domain = null;

    private static boolean useMetric, useTEMetric, useDelay;
    private static int nbQosMetrics;

    // XAMCRA does not record the LSPs. We have to do it here for it.
    private static HashMap<String, Lsp> listOfLsp;

    private static HashMap runningParams = null;

    public final static int SAMCRA = 0;
    public final static int TAMCRA = 1;

    private static boolean DBValid = true;

    public void start(HashMap params) throws AlgorithmInitialisationException {
        try {
            System.loadLibrary("XAMCRA");
        } catch (UnsatisfiedLinkError e){
            throw new LibraryInitialisationException("Cannot load library XAMCRA.");
        }

        int ASID=0;

        runningParams = params;

        // Initialize the list of LSPs
        listOfLsp = new HashMap<String, Lsp>();

        // if XAMCRA is not used with scenarios, the ASID might not be passed adequately
        if (params.get("ASID")==null){
            domain = InterDomainManager.getInstance().getDefaultDomain();
            ASID = domain.getASID();
            logger.warn("You've not specified a domain when starting XAMCRA, default domain with ASID " + ASID + " was used");

        }else{
            ASID = Integer.parseInt((String)params.get("ASID"));
            try{
                domain = InterDomainManager.getInstance().getDomain(ASID);
            }catch(InvalidDomainException e){
                e.printStackTrace();
            }
        }


        List<Node> nodes = domain.getUpNodes();
        List<Link> links = domain.getUpLinks();

        int xamcraVersion = SAMCRA; // specify if we want to use SAMCRA (type = 0) or TAMCRA (type = 1) or ...
        if (params.get("version")!=null) {
            if (((String) params.get("version")).equals("SAMCRA")) {
                logger.info("SAMCRA is used");
                xamcraVersion = SAMCRA;
            }
            else if (((String) params.get("version")).equals("TAMCRA")) {
                logger.info("TAMCRA is used");
                xamcraVersion = TAMCRA;
                logger.error("TAMCRA not Yet implemented.");
                return;
            }
            else {
                logger.warn("The version of XAMCRA you entered is not known...Taking SAMCRA by default !");
            }
        }
        else {
            logger.warn("No version of XAMCRA found...Taking SAMCRA by default !");
        }

        // Initialise the configuration of the QoS metrics...
        nbQosMetrics = 1;
        if (params.get("useDelay") == null) {
            useDelay = false;
        }
        else if (((String) params.get("useDelay")).equals("true")) {
            useDelay = true;
            nbQosMetrics++;
        }
        else if (((String) params.get("useDelay")).equals("false")) {
            useDelay = false;
        }
        else {
            logger.warn("The value of the useDelay parameter has to be true or false. Default false value is used.");
            useDelay = false;
        }

        if (params.get("useMetric") == null) {
            useMetric = false;
        }
        else if (((String) params.get("useMetric")).equals("true")) {
            useMetric = true;
            nbQosMetrics++;
        }
        else if (((String) params.get("useMetric")).equals("false")) {
            useMetric = false;
        }
        else {
            logger.warn("The value of the useMetric parameter has to be true or false. Default false value is used.");
            useMetric = false;
        }

        if (params.get("useTEMetric") == null) {
            useTEMetric = false;
        }
        else if (((String) params.get("useTEMetric")).equals("true")) {
            useTEMetric = true;
            nbQosMetrics++;
        }
        else if (((String) params.get("useTEMetric")).equals("false")) {
            useTEMetric = false;
        }
        else {
            logger.warn("The value of the useTEMetric parameter has to be true or false. Default false value is used.");
            useTEMetric = false;
        }


        try{
            System.out.println("Call of init XAMCRA");
            JNIXAMCRA.jniinitXamcra(nodes.size(), links.size(), nbQosMetrics, xamcraVersion);
        }
        catch(Exception e){
            e.printStackTrace();
            throw new AlgorithmInitialisationException("XAMCRA cannot be started - error in init: " + e.getMessage());
        }


        convertor = domain.getConvertor();

        System.out.println("Adding nodes");

        // adding nodes
        for(Iterator<Node> it1 = nodes.iterator(); it1.hasNext();) {
            Node currentNode = it1.next();
            String nodeId = currentNode.getId();

            try{

                int intnodeId = convertor.getNodeId(nodeId);
                //System.out.println("Call of JNI add node");
                JNIXAMCRA.jniaddNode(intnodeId);
            }
            catch(Exception e){
                throw new AlgorithmInitialisationException("XAMCRA cannot be started - error in addNode: " + e.getMessage());
            }

        }

        System.out.println("Adding links");
        
        // adding links
        for(Iterator<Link> it1 = links.iterator(); it1.hasNext();){
            Link link = it1.next();

            try{

                int srcnodeId = convertor.getNodeId(link.getSrcNode().getId());
                int dstnodeId = convertor.getNodeId(link.getDstNode().getId());

                //System.out.println("call of JNI add link");
                double availableBandwidth = link.getBandwidth() - link.getReservedBandwidth();

                double[] metrics = new double[nbQosMetrics - 1];

                // Initialise the link metrics
                int i = 0;
                if (useDelay) {
                    metrics[i++] = link.getDelay();
                }
                if (useMetric) {
                    metrics[i++] = link.getMetric();
                }
                if (useTEMetric) {
                    metrics[i++] = link.getTEMetric();
                }

                // We give the capacity of the link and not the capacity because the reserved bandwidth is substracted
                // when we add all the LSP present in the topology.
                JNIXAMCRA.jniaddLink(srcnodeId, dstnodeId, link.getBandwidth(), metrics);

            }
            catch(Exception e){
                e.printStackTrace();
                logger.error("Error starting XAMCRA, please check that the topology is not a multi-graph (multiple links between the same pair of nodes)");
                throw new AlgorithmInitialisationException("XAMCRA cannot be started - error in addLink: " + e.getMessage());
            }
        }

        System.out.println("Adding lsps");

        // adding lsps if they exist
        if (domain.getAllLsps().size() != 0){

            List<Lsp> lspsList = domain.getAllLsps();

            for (Iterator<Lsp> it = lspsList.iterator(); it.hasNext();){

                try{
                    //logger.info("Add lsp is called");
                    //System.out.println("Call of JNI add LSP");
                    addLSP(it.next());

                }
                catch(RoutingException e)
                {
                    logger.error(e.getClass().getSimpleName() + ": " + e.getMessage());
                    throw new AlgorithmInitialisationException("XAMCRA cannot be started - error in addLsp: " + e.getMessage());
                }
            }
        }
        DBValid = true;
    }

    /**
     * Cleans all Xamcra data structures
     */
    public void stop() {
        runningParams = null;
        DBValid = false;
        try{
            System.out.println("Call of kill XAMCRA");
            JNIXAMCRA.jnikillXamcra();
        }
        catch(Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Computes paths with XAMCRA for a list of demands.
     * This method just calls the routeLSP method for each demand.
     *
     * @param param the list of the demand specified as a list of LSPPrimaryRoutingParameter
     * @return a list of AddLspAction
     * @throws be.ac.ulg.montefiore.run.totem.repository.model.exception.RoutingException
     * @throws be.ac.ulg.montefiore.run.totem.repository.model.exception.NoRouteToHostException
     */
    public TotemActionList routeNLSP(Domain dom, List<LSPPrimaryRoutingParameter> param) throws RoutingException, NoRouteToHostException, LocalDatabaseException {
        if (domain.getASID() != dom.getASID()){
            throw new LocalDatabaseException("ERROR: ASID from route computation differs from the one loaded into XAMCRA DB");
        }
        TotemActionList fullActionList = new TotemActionList();
        TotemActionList currentActionList;
        for (int i = 0; i < param.size(); i++) {
            currentActionList = routeLSP(domain,param.get(i));
            fullActionList.add(currentActionList.get(0));
        }

        return fullActionList;
    }

    /**
     * Computes a path with XAMCRA for one demand from a source node
     * to a destination node with a bandwidth requirement
     * @param param contains the source, destination, bandwidth, PL, OA, ...
     * @return a list of actions
     * @throws RoutingException
     * @throws NoRouteToHostException
     */
    public TotemActionList routeLSP(Domain dom, LSPPrimaryRoutingParameter param) throws RoutingException, NoRouteToHostException, LocalDatabaseException {
        //System.out.println("Calling route LSP");
        if (!isDBValid()) {
            throw new LocalDatabaseException("Database is invalid. Please restart the algorithm.");
        }

        if (domain.getASID() != dom.getASID()){
            throw new LocalDatabaseException("ERROR: ASID from route computation differs from the one loaded into XAMCRA DB");
        }

        boolean addLSP = false;
        if (param.getRoutingAlgorithmParameter("addLSP")!=null){
            addLSP = Boolean.parseBoolean((String)param.getRoutingAlgorithmParameter("addLSP"));
        }

        String srcNode = param.getSrcNode();
        String dstNode = param.getDstNode();
        float bw = param.getBandwidth();
        int[] path = null;

        String lspId = null;
        if (param.getLspId()==null){
            lspId = IdGenerator.getInstance().generateStringId("Lsp");
        } else {
            lspId = param.getLspId();
        }

        try {
            //System.out.println("Calling JNI compute path (1)");
            convertor.addLspId(lspId);

            int srcnodeId = convertor.getNodeId(srcNode);
            int dstnodeId = convertor.getNodeId(dstNode);

            int addLSPtoDB = 0;
            if (addLSP) {
                addLSPtoDB = 1;
            }

            //System.out.println("Call of JNI compute path");
            double bandwidthConstraint = bw;
            double[] metricsConstraints = new double[nbQosMetrics - 1];

            int i = 0;
            if (useDelay) {
                metricsConstraints[i++] = (param.getRoutingAlgorithmParameter("DelayConstraint") == null) ? Double.MAX_VALUE : Double.parseDouble((String) param.getRoutingAlgorithmParameter("DelayConstraint"));
            }
            if (useMetric) {
                metricsConstraints[i++] = (param.getRoutingAlgorithmParameter("MetricConstraint") == null) ? Double.MAX_VALUE : Double.parseDouble((String) param.getRoutingAlgorithmParameter("MetricConstraint"));
            }
            if (useTEMetric) {
                metricsConstraints[i++] = (param.getRoutingAlgorithmParameter("TEMetricConstraint") == null) ? Double.MAX_VALUE : Double.parseDouble((String) param.getRoutingAlgorithmParameter("TEMetricConstraint"));
            }

            //System.out.println("Calling JNI compute path");
            path = JNIXAMCRA.jnicomputePath(srcnodeId, dstnodeId, bandwidthConstraint, metricsConstraints, addLSPtoDB);
        }
        catch (AddDBException e){
            logger.warn("This primary path failed to add to XAMCRA database!");
            if (logger.isDebugEnabled()){
                e.printStackTrace();
            }
            throw new RoutingException();
        }
        catch (LspAlreadyExistException e){
            logger.warn("Error with lsp ids string to int conversion");
            if (logger.isDebugEnabled()){
                e.printStackTrace();
            }
            throw new RoutingException();
        }
        catch (NoRouteToHostException e){
            logger.warn("Impossible to compute a path for this LSP!");
            if (logger.isDebugEnabled()){
                e.printStackTrace();
            }
            throw new NoRouteToHostException();
        }
        catch (RoutingException e){
            logger.warn("Problem with routing");
            if (logger.isDebugEnabled()){
                e.printStackTrace();
            }
            throw new RoutingException();
        }
        catch (NodeNotFoundException e){
            logger.warn("Node not found!");
            if (logger.isDebugEnabled()){
                e.printStackTrace();
            }
            throw new RoutingException();
        }


        Path returnPath = null;
        try{
            List<Node> nodeList = new ArrayList<Node>(path.length);

            for (int i=0;i<path.length;i++){

                nodeList.add(domain.getNode(convertor.getNodeId(path[i])));

            }

            returnPath = new PathImpl(domain);
            returnPath.createPathFromNode(nodeList);

        }catch(Exception e){
            e.printStackTrace();
        }



        LspImpl lsp = new LspImpl(domain,lspId,bw,returnPath);
        lsp.setInitParameters(param);

        TotemAction addLsp = new AddLspAction(domain,lsp);

        TotemActionList actionList = new TotemActionList();

        actionList.add(addLsp);

        if (addLSP) {
            listOfLsp.put(lsp.getId(), lsp);
        }

        return actionList;
    }

    /**
     * Adds an LSP to XAMCRA database
     * @param lsp the LSP to be added
     * @throws RoutingException
     */
    public void addLSP(Lsp lsp) throws RoutingException{

        if (lsp.isBackupLsp() && domain.useBandwidthSharing()) {
            invalidateDB();
            throw new RoutingException("XAMCRA doesn't support bandwidth sharing.");
        }

        try{
            List<Node> pathnodeList = lsp.getLspPath().getNodePath();


            int[] pathArray = new int[pathnodeList.size()];

            int i=0;
            //logger.info("Path: ");
            for (Iterator<Node> it = pathnodeList.iterator(); it.hasNext(); ){

                pathArray[i++]=convertor.getNodeId(it.next().getId());
                //logger.info(" " + pathArray[i-1]);
            }

            listOfLsp.put(lsp.getId(), lsp);

            //System.out.println("Call of JNI add path");
            JNIXAMCRA.jniaddPath(pathArray,lsp.getReservation());

        }
        catch(AddDBException e){
            e.printStackTrace();
            throw new RoutingException("Failed to add lsp " + lsp.getId() + " to XAMCRA database");
        } catch (NodeNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * print XAMCRA internal DB to the standard output
     */
    public void printDB() {
        JNIXAMCRA.jniprintXamcraDB();
    }

    public List<ParameterDescriptor> getStartAlgoParameters() {
        return (List<ParameterDescriptor>) params.clone();
    }

    public HashMap getRunningParameters() {
        return (runningParams == null) ? null : (HashMap)runningParams.clone();
    }

    public List<ParameterDescriptor> getPrimaryRoutingParameters() {
        return (List<ParameterDescriptor>) routingParams.clone();
    }

    public boolean isDBValid() {
        return DBValid;
    }

    public void invalidateDB() {
        DBValid = false;
    }

    public void restart() {
        HashMap params = getRunningParameters();
        stop();
        try {
            start(params);
        } catch (AlgorithmInitialisationException e) {
            e.printStackTrace();
        }
    }
}
