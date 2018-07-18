package org.mtransit.parser.ca_milton_transit_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// http://icreateopendata.public.esolutionsgroup.ca/
// http://icreateopendata.public.esolutionsgroup.ca/home/details/9765286c-48b4-417c-8c23-3a64334e6c04
// http://icreateopendata.public.esolutionsgroup.ca/home/ServeFile/9765286c-48b4-417c-8c23-3a64334e6c04?FileType=7
public class MiltonTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-milton-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new MiltonTransitBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating Milton Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating Milton Transit bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if ("Not In Service".equalsIgnoreCase(gTrip.getTripHeadsign())) {
			return true;
		}
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		return super.excludeRoute(gRoute);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String A = "A";
	private static final String B = "B";
	private static final String C = "C";

	private static final String EB = "EB";
	private static final String WB = "WB";
	private static final String AM = "AM";
	private static final String PM = "PM";

	private static final long ROUTE_ID_ENDS_WITH_A = 10_000L;
	private static final long ROUTE_ID_ENDS_WITH_B = 20_000L;
	private static final long ROUTE_ID_ENDS_WITH_C = 30_000L;

	@Override
	public long getRouteId(GRoute gRoute) {
		if (gRoute.getRouteShortName() != null && gRoute.getRouteShortName().length() > 0 && Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return Long.parseLong(gRoute.getRouteShortName()); // use route short name as route ID
		}
		int indexOf;
		indexOf = gRoute.getRouteShortName().indexOf(EB);
		if (indexOf >= 0) {
			return Long.parseLong(gRoute.getRouteShortName().substring(0, indexOf)); // use route short name as route ID
		}
		indexOf = gRoute.getRouteShortName().indexOf(WB);
		if (indexOf >= 0) {
			return Long.parseLong(gRoute.getRouteShortName().substring(0, indexOf)); // use route short name as route ID
		}
		indexOf = gRoute.getRouteShortName().indexOf(AM);
		if (indexOf >= 0) {
			return Long.parseLong(gRoute.getRouteShortName().substring(0, indexOf)); // use route short name as route ID
		}
		indexOf = gRoute.getRouteShortName().indexOf(PM);
		if (indexOf >= 0) {
			return Long.parseLong(gRoute.getRouteShortName().substring(0, indexOf)); // use route short name as route ID
		}
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		if (matcher.find()) {
			long id = Long.parseLong(matcher.group());
			if (gRoute.getRouteShortName().endsWith(A)) {
				return ROUTE_ID_ENDS_WITH_A + id;
			} else if (gRoute.getRouteShortName().endsWith(B)) {
				return ROUTE_ID_ENDS_WITH_B + id;
			} else if (gRoute.getRouteShortName().endsWith(C)) {
				return ROUTE_ID_ENDS_WITH_C + id;
			}
		}
		System.out.printf("\nUnexpected route ID for %s!\n", gRoute);
		System.exit(-1);
		return -1l;
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		int indexOf;
		indexOf = gRoute.getRouteShortName().indexOf(EB);
		if (indexOf >= 0) {
			return gRoute.getRouteShortName().substring(0, indexOf);
		}
		indexOf = gRoute.getRouteShortName().indexOf(WB);
		if (indexOf >= 0) {
			return gRoute.getRouteShortName().substring(0, indexOf);
		}
		indexOf = gRoute.getRouteShortName().indexOf(AM);
		if (indexOf >= 0) {
			return gRoute.getRouteShortName().substring(0, indexOf);
		}
		indexOf = gRoute.getRouteShortName().indexOf(PM);
		if (indexOf >= 0) {
			return gRoute.getRouteShortName().substring(0, indexOf);
		}
		return super.getRouteShortName(gRoute);
	}

	private static final String ROUTE_COLOR_SCHOOL = "FFD800"; // School bus yellow

	@Override
	public String getRouteColor(GRoute gRoute) {
		String routeColor = gRoute.getRouteColor();
		if ("000000".equalsIgnoreCase(routeColor)) {
			routeColor = null;
		}
		if (StringUtils.isEmpty(routeColor)) {
			if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
				int rsn = Integer.parseInt(gRoute.getRouteShortName());
				switch (rsn) {
				case 1:
					return "EF2E31"; // TODO really?
				case 2:
					return "263D96";
				case 3:
					return "195B1C";
				case 4:
					return "EC008C";
				case 5:
					return "FBBF15";
				case 6:
					return "A32B9B";
				case 7:
					return "00ADEF";
				case 8:
					return "6EC72D";
				case 9:
					return "F57814";
				case 30:
					return null; // TODO ?
				case 31:
					return null; // TODO ?
				case 32:
					return null; // TODO ?
				case 50:
					return ROUTE_COLOR_SCHOOL;
				case 51:
					return ROUTE_COLOR_SCHOOL;
				case 52:
					return ROUTE_COLOR_SCHOOL;
				}
			}
			if ("1A".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "EF2E31";
			}
			if ("1B".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "B4131D";
			}
			if ("1C".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "AB6326";
			}
			System.out.printf("\n%s: Unexpected route color for %s!\n", gRoute.getRouteId(), gRoute);
			System.exit(-1);
			return null;
		}
		return routeColor;
	}

	private static final Pattern ENDS_WITH_BOUNDS = Pattern.compile("( (eastbound|westbound)$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern AM_PM = Pattern.compile("(^|\\s){1}(am|pm)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String AM_PM_REPLACEMENT = "$1$3";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (!StringUtils.isEmpty(gRoute.getRouteLongName())) {
			return cleanRouteLongName(gRoute.getRouteLongName());
		}
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			case 1:
				return "Industrial";
			case 2:
				return "Main";
			case 3:
				return "Trudeau";
			case 4:
				return "Thompson / Clark";
			case 5:
				return "Yates";
			case 6:
				return "Scott";
			case 7:
				return "Harrison";
			case 8:
				return "Willmott";
			case 9:
				return "Ontario South";
			case 30:
				return "Milton West Zone";
			case 31:
				return "Milton Central Zone";
			case 32:
				return "Milton East Zone";
			case 50:
				return "School Special";
			case 51:
				return "School Special";
			case 52:
				return "School Special";
			}
		}
		if ("1A".equalsIgnoreCase(gRoute.getRouteShortName())) {
			return "Industrial";
		}
		if ("1B".equalsIgnoreCase(gRoute.getRouteShortName())) {
			return "Industrial";
		}
		if ("1C".equalsIgnoreCase(gRoute.getRouteShortName())) {
			return "Industrial";
		}
		System.out.printf("\n%s: Unexpected route long name for %s!\n", gRoute.getRouteId(), gRoute);
		System.exit(-1);
		return null;
	}

	private String cleanRouteLongName(String routeLongName) {
		if (Utils.isUppercaseOnly(routeLongName, true, true)) {
			routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		}
		routeLongName = ENDS_WITH_BOUNDS.matcher(routeLongName).replaceAll(StringUtils.EMPTY);
		routeLongName = AM_PM.matcher(routeLongName).replaceAll(AM_PM_REPLACEMENT);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_GREEN = "00615C"; // GREEN (like color on buses)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String MILTON_GO = "Milton Go";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(1L + ROUTE_ID_ENDS_WITH_A, new RouteTripSpec(1L + ROUTE_ID_ENDS_WITH_A, // 1A
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // Milton GO
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // No 5 Side Road
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2316", // No 5 Side (RR 25)
								"2317", // Regional Road 25 (Peddle Rd)
								"2318", // Regional Road 25 (Peddle Rd)
								"2369", // Regional Road 25 (James Snow)
								"2370", // James Snow
								"2373", // ++
								"2203", // Milton GO Station
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2203", // Milton GO Station
								"2333", // Highway 401 Park-And-Ride
								"2367", // James Snow
								"2368", // Regional Road 25 (Escarpment Way)
								"2316", // No 5 Side (RR 25)
						})) //
				.compileBothTripSort());
		map2.put(1L + ROUTE_ID_ENDS_WITH_B, new RouteTripSpec(1L + ROUTE_ID_ENDS_WITH_B, // 1B
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // RR 25
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Milton GO
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2351", // Milton GO Station
								"2075", // ++
								"2314", // James Snow
								"2315", // Regional Road 25 (James Snow)
								"2316", // No 5 Side (RR 25)
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2316", // No 5 Side (RR 25)
								"2317", // Regional Road 25 (Peddle Rd)
								"2318", // Regional Road 25 (Peddle Rd)
								"2319", // Regional Road 25 (Escarpment Way)
								"2320", // James Snow
								"2335", // ++
								"2351" // Milton GO Station
						})) //
				.compileBothTripSort());
		map2.put(1L + ROUTE_ID_ENDS_WITH_C, new RouteTripSpec(1L + ROUTE_ID_ENDS_WITH_C, // 1C
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // High Pt
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Milton GO
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2203", // Milton GO Station
								"2354", // ++
								"2366", // !=
								"2333", // <> Highway 401 Park-And-Ride
								"2324", // != Regional Road 25
								"2326", // High Point (Parkhill Dr)
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2326", // High Point (Parkhill Dr)
								"2327", // Parkhill
								"2331", // High Point
								"2332", // != Regional Road 25
								"2333", // <> Highway 401 Park-And-Ride
								"2334", // !=
								"2347", // ++
								"2169", // Milton GO Station
						})) //
				.compileBothTripSort());
		map2.put(2L, new RouteTripSpec(2L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // Milton Crossroads Ctr
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Milton Hospital
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2038", // Bronte (Milton Hospital)
								"2039", // Farmstead
								"2044", // Heslop (Commercial)
								"2061", // !=
								"2017", // <>
								"2169", // Milton GO Station
								"2019", // <>
								"2063", // !=
								"2001", // Milton Crossroads at Walmart
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2001", // Milton Crossroads at Walmart
								"2004", // Thompson
								"2016", // !=
								"2017", // <>
								"2169", // <> Milton GO Station
								"2019", // <>
								"2020", // !=
								"2033", // Heslop (Bronte)
								"2037", // Derry
								"2038", // Bronte (Milton Hospital)
						})) //
				.compileBothTripSort());
		map2.put(3L, new RouteTripSpec(3L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // Milton GO
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Louis St Laurent
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2286", // Fourth Line (Louis St Laurent)
								"2287", // Stewart
								"2298", // ++
								"2123", // Milton GO Station
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2123", // Milton GO Station
								"2273", // ++
								"2283", // Earl
								"2285", // Louis St Laurent
								"2286", // Fourth Line (Louis St Laurent)
						})) //
				.compileBothTripSort());
		map2.put(4L, new RouteTripSpec(4L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // Milton GO
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Armstrong // 4th Ln & Louis St Laurent
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2147", // Fourth Line (Louis St Laurent)
								"2159", // ++
								"2168", // ==
								"2123", // != Milton GO Station => CONTINUE
								"2169", // != Milton GO Station => END
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2123", // Milton GO Station
								"2134", // ++
								"2139", // Armstrong (Ferguson)
								"2147", // Fourth Line (Louis St Laurent)
						})) //
				.compileBothTripSort());
		map2.put(5L, new RouteTripSpec(5L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // Milton GO
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Hepburn
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2225", // Hepburn (Philbrook)
								"2228", // Yates (Hepburn)
								"2239", // ++
								"2169", // Milton GO Station
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2169", // Milton GO Station
								"2214", // ++
								"2225", // Hepburn (Philbrook)
						})) //
				.compileBothTripSort());
		map2.put(6L, new RouteTripSpec(6L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // Milton GO
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Derry
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2094", // Derry (Savoline)
								"2096", // Hinchey
								"2052", // == Bronte
								"2054", // != Brown
								"2110", // != Fulton
								"2055", // != Mill
								"2057", // != TransCab Transfer Point Westbound
								"2058", // == Court
								"2203" // Milton GO Station
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2203", // Milton GO Station
								"2024", // ==
								"2080", // !=
								"2028", // !=
								"2029", // ==
								"2083", // Main
								"2091", // Weston
								"2092", // Derry
								"2093", // Scott
								"2094", // Derry (Savoline)
						})) //
				.compileBothTripSort());
		map2.put(7L, new RouteTripSpec(7L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // Milton GO
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Savoline
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2191", // Savoline (Derry)
								"2240", // ++
								"2203", // Milton GO Station
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2203", // Milton GO Station
								"2180", // Scott at Derry
								"2185", // Dymott at Savoline
								"2191", // Savoline (Derry)
						})) //
				.compileBothTripSort());
		map2.put(8L, new RouteTripSpec(8L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // Milton GO
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Ruhl
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2254", // Louis St Laurent (Bronte)
								"2259", // Ruhl (Farmstead)
								"2200", // ++
								"2169", // Milton GO Station
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2169", // Milton GO Station
								"2175", // Ontario
								"2250", // Ruhl (Bronte)
								"2254", // Louis St Laurent (Bronte)
						})) //
				.compileBothTripSort());
		map2.put(9L, new RouteTripSpec(9L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // Milton GO
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Britannia
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2393", // Britannia (Farmstead)
								"2394", // RR 25 at Louis St Laurent (SE)
								"2017", // ==
								"2169", // Milton GO Station => SOUTH
								"2123", // Milton GO Station
								"2203", // Milton GO Station
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2169", // Milton GO Station
								"2391", // Etheridge at Orr
								"2392", // Farmstead (Etheridge)
								"2393", // Britannia (Farmstead)
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public String cleanStopOriginalId(String gStopId) {
		gStopId = STARTS_WITH_MI.matcher(gStopId).replaceAll(StringUtils.EMPTY);
		return gStopId;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId() == null ? 0 : gTrip.getDirectionId());
	}

	private static final Pattern STARTS_WITH_TO = Pattern.compile("(^to )", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = STARTS_WITH_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 4L) {
			if (Arrays.asList( //
					"Louis St. Laurent & 4th", //
					"Regional Rd 25 & Britannia" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Louis St. Laurent & 4th", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 9L) {
			if (Arrays.asList( //
					"High Pt", //
					"Regional Rd 25 & Britannia" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Regional Rd 25 & Britannia", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 1L + ROUTE_ID_ENDS_WITH_A) { // 1A
			if (Arrays.asList( //
					"R.R. 25 & No. 5 Side Rd", //
					MILTON_GO).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MILTON_GO, mTrip.getHeadsignId());
				return true;
			}
		}
		System.out.printf("\n%s: Unexpected trips to merge: %s & %s!\n", mTrip.getRouteId(), mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanSlashes(gStopName);
		gStopName = CleanUtils.removePoints(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	public static final Pattern STARTS_WITH_MI = Pattern.compile("((^){1}(mi))", Pattern.CASE_INSENSITIVE);

	@Override
	public int getStopId(GStop gStop) {
		String stopId = gStop.getStopId();
		stopId = STARTS_WITH_MI.matcher(stopId).replaceAll(StringUtils.EMPTY);
		return Integer.parseInt(stopId);
	}
}
