//package netention.possibility;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import netention.core.NObject;
//import netention.core.SpacePoint;
//import netention.core.TimePoint;
//import netention.core.TimeRange;
//import org.apache.commons.math3.ml.clustering.CentroidCluster;
//import org.apache.commons.math3.ml.clustering.DoublePoint;
//import org.apache.commons.math3.ml.clustering.FuzzyKMeansClusterer;
//import org.apache.commons.math3.ml.distance.DistanceMeasure;
//
///**
// *
// * @author me
// * 
// * http://commons.apache.org/proper/commons-math/javadocs/api-3.3/org/apache/commons/math3/ml/clustering/FuzzyKMeansClusterer.html
// */
//public class SpacetimeTagPlan {
//    
//    public final TagVectorMapping mapping;
//    public final List<Goal> goals = new ArrayList();
//    
//    //Parameters, can be changed between computations
//    protected double timeWeight = 1.0;  //normalized to seconds
//    protected double spaceWeight = 1.0; //meters
//    protected double altWeight = 1.0; //meters
//    protected double tagWeight = 1.0;  //weight per each individual tag    
//    protected double minPossibilityTagStrength = 0.02; //minimum strength that a resulting tag must be to be added to a generated Possibility
//
//    
//    //internal
//    private final boolean time;
//    private final boolean space;
//    private final boolean spaceAltitude;
//    private final boolean tags;
//    public final List<NObject> objects;
//    private double timeWeightNext = timeWeight;
//    private double spaceWeightNext = spaceWeight;
//    private double tagWeightNext = tagWeight;
//    private double altWeightNext = altWeight;
//    
//    public static class TagVectorMapping extends ArrayList<String> {
//        public final long timePeriod;
//        private static final TimePoint NullTimePoint = new TimePoint(-1);
//        double[] min, max;
//        private int tagIndex = -1; //index where tags begin in the mapping; they continue until the end
//        
//        /**
//         * 
//         * @param timePeriod  in ms (unixtime)
//         */
//        public TagVectorMapping(long timePeriod) {
//            this.timePeriod = timePeriod;
//        }
//        
//        /** reset before generating a new sequence of goals */
//        public void reset() {
//            min = max = null;
//        }
//        
//        public List<Goal> newGoals(NObject o) {
//            boolean firstGoal = false;
//            if (min == null) {
//                firstGoal = true;
//                min = new double[size()];
//                max = new double[size()];                
//            }
//            
//            List<Goal> goals = new LinkedList();
//            Map<String,Double> ts = o.getTagStrengths();
//            
//            
//            SpacePoint sp = null;
//                        
//            List<TimePoint> times = times = new ArrayList(1);
//            
//            if (get(0).equals("time")) {
//                //convert time ranges to a set of time points
//                TimeRange tr = TimeRange.get(o);
//                if (tr != null) {                    
//                    times.addAll(tr.discretize(timePeriod));
//                }
//                else {
//                    TimePoint tp = TimePoint.get(o);
//                    if (tp!=null) {
//                        times.add(tp);
//                    }
//                    else {
//                        //no timepoint, ignore this NObject
//                        return goals;
//                    }
//                }
//            }
//            else {
//                //add a null timepoint so the following iteration occurs
//                times.add(NullTimePoint);
//            }
//            
//            tagIndex = -1;
//            
//            for (TimePoint currentTime : times) {
//                
//                double[] d = new double[this.size()];
//                int i = 0;
//                
//                for (String s : this) {
//                    if (s.equals("lat")) {
//                        sp = SpacePoint.get(o);
//                        if (sp==null) {
//                            //this nobject is invalid, return; goals will be empty
//                            return goals;
//                        }
//                        d[i] = sp.lat;
//                    }
//                    else if (s.equals("lon")) {
//                        d[i] = sp.lon;
//                    }
//                    else if (s.equals("time")) {
//                        d[i] = currentTime.at;
//                    }
//                    else if (s.equals("alt")) {
//                        d[i] = sp.alt;
//                    }
//                    else {
//                        if (tagIndex == -1) {
//                            tagIndex = i;
//                        }
//                        Double strength = ts.get(s);
//                        if (strength!=null) {
//                            d[i] = strength;
//                        }
//                    }
//                    i++;                    
//                }
//                if (firstGoal) {
//                    System.arraycopy(d, 0, min, 0, d.length);
//                    System.arraycopy(d, 0, max, 0, d.length);
//                }
//                else {
//                    for (int j = 0; j < d.length; j++) {
//                        if (d[j] < min[j]) min[j] = d[j];
//                        if (d[j] > max[j]) max[j] = d[j];
//                    }
//                }
//                    
//                goals.add(new Goal(o, this, d));
//            }
//
//            return goals;
//        }
//
//        /** normalize (to 0..1.0) a collection of Goals with respect to the min/max calculated during the prior goal generation */
//        public void normalize(Collection<Goal> goals) {
//            
//            for (Goal g : goals) {
//                double d[] = g.getPoint();
//                for (int i = 0; i < d.length; i++) {
//                    double MIN = min[i];
//                    double MAX = max[i];
//                    if (MIN!=MAX) {
//                        d[i] = (d[i] - MIN) / (MAX-MIN);
//                    }
//                    else {
//                        d[i] = 0.5;
//                    }
//                }
//            }
//        }
//
//        public void denormalize(Goal g) {
//            denormalize(g.getPoint());
//        }
//        
//        public void denormalize(double[] d) {
//            for (int i = 0; i < d.length; i++) {
//                double MIN = min[i];
//                double MAX = max[i];
//                if (MIN!=MAX) {
//                    d[i] = d[i] * (MAX-MIN) + MIN;
//                }
//                else {
//                    d[i] = MIN;
//                }
//            }            
//
//            //normalize tags against each other
//            if (tagIndex >= d.length) return;
//            
//            double min, max;
//            min = max = d[tagIndex];
//            for (int i = tagIndex+1; i < d.length; i++) {
//                if (d[i] > max) max = d[i];
//                if (d[i] < min) min = d[i];
//            }
//            if (min!=max) {
//                for (int i = tagIndex; i < d.length; i++) {
//                    d[i] = (d[i] - min)/(max-min);
//                }
//            }
//            
//        }
//        
//        
//    }
//    
//    /** a point in goal-space; the t parameter is included for referencing what the dimensions mean */
//    public static class Goal extends DoublePoint {
//        private final TagVectorMapping mapping;
//        
//        /** the implicated object */
//        private final NObject object;
//
//        public Goal(NObject o, TagVectorMapping t, double[] v) {
//            super(v);
//            this.object = o;
//            this.mapping = t;            
//        }
//        
//        
//    }
//    
//    //TODO add a maxDimensions parameter that will exclude dimensions with low aggregate strength
//    
//    //TODO support negative strengths to indicate avoidance
//    
//    /**
//     * 
//     * @param n list of objects
//     * @param tags whether to involve tags
//     * @param timePeriod  time period of discrete minimum interval; set to zero to not involve time as a dimension
//     * @param space whether to involve space latitude & longitude
//     * @param spaceAltitude whether to involve space altitude
//     */
//    public SpacetimeTagPlan(List<NObject> n, boolean tags, long timePeriod, boolean space, boolean spaceAltitude) {
//        
//        this.objects = n;
//        
//        //1. compute mapping
//        this.mapping = new TagVectorMapping(timePeriod);
//        
//        this.time = timePeriod > 0;
//        this.space = space;
//        this.spaceAltitude = spaceAltitude;
//        this.tags = tags;
//        
//                
//        if (this.time)
//            mapping.add("time");
//        if (space) {
//            mapping.add("lat");
//            mapping.add("lon");
//        }
//        if (spaceAltitude) {
//            mapping.add("alt");
//        }
//        
//        //TODO filter list of objects according to needed features for the clustering parameters
//        
//        if (tags) {
//            Set<String> uniqueTags = new HashSet();
//            for (NObject o : n) {
//                uniqueTags.addAll(o.getTags());
//            }
//            mapping.addAll(uniqueTags);
//        }
//        
//            
//    }
//    
//    public interface PlanResult {
//        public void onFinished(SpacetimeTagPlan plan, List<Possibility> possibilities);        
//        public void onError(SpacetimeTagPlan plan, Exception e);
//    }
//    
//    public void update(int numCentroids, int maxIterations, double fuzziness, PlanResult r) {
//        try {
//            List<Possibility> result = compute(numCentroids, maxIterations, fuzziness);
//            r.onFinished(this, result);
//            return;
//        }
//        catch (Exception e) {
//            r.onError(this, e);
//        }
//    }
//    
//    protected synchronized List<Possibility> compute(int numCentroids, int maxIterations, double fuzziness) {
//        goals.clear();
//        mapping.reset();
//        
//        this.spaceWeight = this.spaceWeightNext;
//        this.altWeight = this.altWeightNext;
//        this.timeWeight = this.timeWeightNext;
//        this.tagWeight = this.tagWeightNext;
//        
//        //2. compute goal vectors 
//        for (NObject o : objects) {
//            goals.addAll(mapping.newGoals(o));
//        }
//        
//        //3. normalize
//        mapping.normalize(goals);
//
//        
//        //4. distance function
//        DistanceMeasure distanceMetric = new DistanceMeasure() {
//
//            @Override
//            public double compute(double[] a, double[] b) {
//                double dist = 0;
//                int i = 0;
//                
//                if (time) {
//                    dist += Math.abs(a[i] - b[i]) * timeWeight;
//                    i++;
//                }
//                if (space) {
//                    //TODO use earth surface distance measurement on non-normalized space lat,lon coordinates
//
//                    if (spaceWeight!=0) {
//                        double dx = Math.abs(a[i] - b[i]);
//                        i++;
//                        double dy = Math.abs(a[i] - b[i]);
//                        i++;
//
//                        double ed = Math.sqrt( dx*dx + dy*dy );
//                        dist += ed * spaceWeight;
//                    }
//                    else {
//                        i+=2;
//                    }
//                }
//                if (spaceAltitude) {
//                    dist += Math.abs(a[i] - b[i]) * altWeight;
//                    i++;
//                }
//                if (tags) {
//                    if ((a.length > 0) && (tagWeight!=0)) {
//                        double tagWeightFraction = tagWeight / (a.length);
//                        for ( ;i < a.length; i++) {
//                            dist += Math.abs(a[i] - b[i]) * tagWeightFraction;
//                        }
//                    }
//                }
//                
//                return dist;
//            }
//            
//        };
//        
//        //5. cluster
//        FuzzyKMeansClusterer<Goal> clusterer = new FuzzyKMeansClusterer<Goal>(numCentroids, fuzziness, maxIterations, distanceMetric);
//        List<CentroidCluster<Goal>> centroids = clusterer.cluster(goals); 
//        
//        //6. denormalize and return annotated objects
//        for (Goal g : goals) {
//            mapping.denormalize(g);
//        }
//        
//        return getPossibilities(centroids);
//    }
//    
//    public TagVectorMapping getMapping() {
//        return mapping;
//    }
//    
//    public class Possibility extends NObject {
//        private final double[] center;
//
//        public Possibility(double[] center) {
//            this.center = center;
//        }
//        
//        public double[] getCenter() {
//            return center;
//        }
//        
//        
//    }
//    
//    protected List<Possibility> getPossibilities(List<CentroidCluster<Goal>> centroids) {
//        List<Possibility> l = new ArrayList(centroids.size());
//        
//        for (CentroidCluster<Goal> c : centroids) {
//            double[] point = c.getCenter().getPoint();
//            mapping.denormalize(point);
//            
//            Possibility p = new Possibility(point);
//            int i = 0;
//            if (time) {
//                long when = (long)point[i++];
//
//                //TODO use timerange based on discretizing period duration?
//                p.add("when", new TimePoint((long)when));
//            }
//            SpacePoint s = null;
//            if (space) {
//                double lat = point[i++];
//                double lon = point[i++];
//                s = new SpacePoint(lat, lon);                    
//                p.add("where", s);
//            }
//
//
//            if (spaceAltitude) {
//                double alt = point[i++];                    
//                if (s == null) {
//                    s = new SpacePoint(0,0,alt);
//                    p.add("where", s);
//                }
//                else
//                    s.alt = alt;
//            }
//
//
//            if (tags) {
//                for ( ;i < point.length; i++) {
//                    double strength = point[i];
//                    if (strength > minPossibilityTagStrength) {
//                        String tag = mapping.get(i);
//                        p.value(tag, strength);
//                    }
//                }
//            }                            
//            
//            l.add(p);
//        }
//        
//        return l;
//    }
//
//    public void setTimeWeight(double timeWeight) {        this.timeWeightNext = timeWeight;    }
//    public void setSpaceWeight(double spaceWeight) {        this.spaceWeightNext = spaceWeight;    }
//    public void setTagWeight(double tagWeight) {       this.tagWeightNext = tagWeight;    }
//    public void setAltWeight(double altWeight) {        this.altWeightNext = altWeight;    }
//    public double getAltWeight() {  return altWeight;   }
//    public double getSpaceWeight() { return spaceWeight;   }
//    public double getTagWeight() {  return tagWeight;    }
//    public double getTimeWeight() { return timeWeight;    }
//     
//    
//    
//    
//}
