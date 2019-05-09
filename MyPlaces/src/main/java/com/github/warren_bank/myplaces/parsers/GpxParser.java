package com.github.warren_bank.myplaces.parsers;

import com.github.warren_bank.myplaces.models.WaypointListItem;

    // =====================================================
    // JOOX api:
    //     https://www.jooq.org/products/jOOX/javadoc/current/allclasses-noframe.html
    //
    //     https://www.jooq.org/products/jOOX/javadoc/current/org/joox/JOOX.html
    //     https://www.jooq.org/products/jOOX/javadoc/current/org/joox/Match.html
    //     https://www.jooq.org/products/jOOX/javadoc/current/org/joox/Each.html
    //     https://www.jooq.org/products/jOOX/javadoc/current/org/joox/Context.html
    //     https://www.jooq.org/products/jOOX/javadoc/current/org/joox/Filter.html
    //     https://www.jooq.org/products/jOOX/javadoc/current/org/joox/FastFilter.html
    //
    //     https://docs.oracle.com/javase/8/docs/api/org/w3c/dom/Element.html
    // =====================================================
import org.joox.JOOX;
import org.joox.Match;
import org.joox.Each;
import org.joox.Context;
import org.joox.Filter;
import org.joox.FastFilter;

import org.w3c.dom.Element;

import java.util.ArrayList;

public class GpxParser extends AbstractParser {
    protected int nonce;

    private static final FastFilter is_wpt   = JOOX.tag("wpt",   true);
    private static final FastFilter is_rtept = JOOX.tag("rtept", true);
    private static final FastFilter is_trkpt = JOOX.tag("trkpt", true);

    private static final FastFilter is_name  = JOOX.tag("name",  true);
    private static final FastFilter is_desc  = JOOX.tag("desc",  true);

    private static final Filter is_point_tag = new Filter() {
        @Override
        public boolean filter(Context context) {
            return (
                is_wpt.filter(  context) ||
                is_rtept.filter(context) ||
                is_trkpt.filter(context)
            );
        }
    };

    private static final Filter is_name_tag  = new Filter() {
        @Override
        public boolean filter(Context context) {
            return (
                is_name.filter(context) ||
                is_desc.filter(context)
            );
        }
    };

    public GpxParser(String filepath) {
        super(filepath);
    }

    @Override
    public ArrayList<WaypointListItem> parse() {
        ArrayList<WaypointListItem> arrayList = new ArrayList<WaypointListItem>();
        this.nonce = 0;

        // =================================================
        // GPX spec:
        //     https://www.topografix.com/gpx_manual.asp
        // =================================================

        Match points;
        try {
            points = getXmlRoot().find(is_point_tag);
        }
        catch(Exception e) {
            return arrayList;
        }

        // =================================================
        // required attributes: "lat", "lon"
        // optional child nodes: <name>, <desc>
        // =================================================

        points.each(new Each(){
            @Override
            public void each(Context context) {
                Element el = context.element();

                String lat = el.getAttribute("lat").trim();
                String lon = el.getAttribute("lon").trim();
                if ((lat == "") || (lon == "")) return;

                String name = JOOX.$(el).find(is_name_tag).text();  // first value, or null if not found
                if (name != null) {
                    name = name.trim();
                }

                arrayList.add(
                    new WaypointListItem(lat, lon, name, GpxParser.this.nonce++)
                );
            }
        });

        return arrayList;
    }
}
