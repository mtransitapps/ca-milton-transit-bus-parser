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
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String A = "A";
	private static final String B = "B";
	private static final String EB = "EB";
	private static final String WB = "WB";
	private static final String AM = "AM";
	private static final String PM = "PM";

	private static final long ROUTE_ID_ENDS_WITH_A = 10000l;
	private static final long ROUTE_ID_ENDS_WITH_B = 20000l;

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
		matcher.find();
		long id = Long.parseLong(matcher.group());
		if (gRoute.getRouteShortName().endsWith(A)) {
			return ROUTE_ID_ENDS_WITH_A + id;
		} else if (gRoute.getRouteShortName().endsWith(B)) {
			return ROUTE_ID_ENDS_WITH_B + id;
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

	private static final Pattern ENDS_WITH_BOUNDS = Pattern.compile("( (eastbound|westbound)$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern AM_PM = Pattern.compile("(^|\\s){1}(am|pm)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String AM_PM_REPLACEMENT = "$1$3";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName().toLowerCase(Locale.ENGLISH);
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

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(1l + ROUTE_ID_ENDS_WITH_A, new RouteTripSpec(1l + ROUTE_ID_ENDS_WITH_A, // 1A
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // Milton GO
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // No 5 Side Road
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2351", "2333", "2317" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2318", "2373", "2351" })) //
				.compileBothTripSort());
		map2.put(1l + ROUTE_ID_ENDS_WITH_B, new RouteTripSpec(1l + ROUTE_ID_ENDS_WITH_B, // 1B
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // RR 25
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Milton GO
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2351", "2075", "2315" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2316", "2335", "2351" })) //
				.compileBothTripSort());
		map2.put(3l, new RouteTripSpec(3l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // Milton GO
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Louis St Laurent
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2286", "2298", "2123" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2123", "2273", "2285" })) //
				.compileBothTripSort());
		map2.put(4l, new RouteTripSpec(4l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // Milton GO
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Armstrong
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2147", "2159", "2123" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2123", "2134", "2147" })) //
				.compileBothTripSort());
		map2.put(5l, new RouteTripSpec(5l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // Milton GO
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Hepburn
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "2228", "2239", "2203" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "2203", "2214", "2228" })) //
				.compileBothTripSort());
		map2.put(6l, new RouteTripSpec(6l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // Milton GO
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Derry
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2094", "2108", //
								"2103", //
								"2060", //
								"2079" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2079", //
								"2021", //
								"2093", //
								"2083", "2094" })) //
				.compileBothTripSort());
		map2.put(7l, new RouteTripSpec(7l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // Milton GO
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Savoline
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2185", "2196", "2169" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2169", "2180", "2185" })) //
				.compileBothTripSort());
		map2.put(8l, new RouteTripSpec(8l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // Milton GO
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Ruhl
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "2254", "2196", "2169" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "2169", "2175", "2254" })) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
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
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()));
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	private static final String _2EB = "2EB";
	private static final String _2WB = "2WB";

	private static final String BLUE = "Blue";
	private static final String GREEN = "Green";

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		if (mRoute.getId() == 2l) {
			if (gTrip.getRouteId().equals(_2EB)) {
				mTrip.setHeadsignDirection(MDirectionType.EAST);
				return;
			} else if (gTrip.getRouteId().equals(_2WB)) {
				mTrip.setHeadsignDirection(MDirectionType.WEST);
				return;
			}
		} else if (mRoute.getId() == 40l) {
			mTrip.setHeadsignString(BLUE, 0);
			return;
		} else if (mRoute.getId() == 41l) {
			mTrip.setHeadsignString(GREEN, 0);
			return;
		}
		if (gTrip.getRouteId().endsWith(AM)) {
			mTrip.setHeadsignString(AM, 0);
			return;
		} else if (gTrip.getRouteId().endsWith(PM)) {
			mTrip.setHeadsignString(PM, 1);
			return;
		}
		System.out.printf("\n%s: Unexpected trip %s!\n", mRoute.getId(), gTrip);
		System.exit(-1);
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern AT = Pattern.compile("((^|\\W){1}(at)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String AT_REPLACEMENT = "$2/$4";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		gStopName = AT.matcher(gStopName).replaceAll(AT_REPLACEMENT);
		gStopName = CleanUtils.cleanSlashes(gStopName);
		gStopName = CleanUtils.removePoints(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
