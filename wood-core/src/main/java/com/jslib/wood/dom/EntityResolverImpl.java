package com.jslib.wood.dom;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * XHTML entity resolver.
 * 
 * @author Iulian Rotaru
 */
final class EntityResolverImpl implements EntityResolver
{
  /** Entity resolvers for XHTML documents. */
  private static final Map<String, String> map;
  static {
    map = new HashMap<>();
    map.put("-//W3C//DTD XHTML 1.1//EN", Resources.XHTML11_DTD);
    map.put("-//W3C//DTD XHTML 1.0 Strict//EN", Resources.XHTML1_STRICT_DTD);
    map.put("-//W3C//DTD XHTML 1.0 Transitional//EN", Resources.XHTML1_TRANSITIONAL_DTD);
    map.put("-//W3C//ELEMENTS XHTML Inline Style 1.0//EN", Resources.XHTML_INLSTYLE_1_MOD);
    map.put("-//W3C//ENTITIES Latin 1 for XHTML//EN", Resources.XHTML_LAT1_ENT);
    map.put("-//W3C//ENTITIES Symbols for XHTML//EN", Resources.XHTML_SYMBOL_ENT);
    map.put("-//W3C//ENTITIES Special for XHTML//EN", Resources.XHTML_SPECIAL_ENT);
    map.put("-//W3C//ENTITIES XHTML Modular Framework 1.0//EN", Resources.XHTML_FRAMEWORK_1_MOD);
    map.put("-//W3C//ENTITIES XHTML Datatypes 1.0//EN", Resources.XHTML_DATATYPES_1_MOD);
    map.put("-//W3C//ENTITIES XHTML Qualified Names 1.0//EN", Resources.XHTML_QNAME_1_MOD);
    map.put("-//W3C//ENTITIES XHTML Intrinsic Events 1.0//EN", Resources.XHTML_EVENTS_1_MOD);
    map.put("-//W3C//ELEMENTS XHTML Events Basic 1.0//EN", Resources.XHTML_EVENTS_BASIC_1_MOD);
    map.put("-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN", Resources.WEB_APP_2_3_DTD);
  }

  /**
   * Get input source for entity definition file identified by public and system ID.
   */
  @Override
  public InputSource resolveEntity(String publicId, String systemId)
  {
    String r = map.get(publicId);
    return r == null ? null : new InputSource(Resources.stream(r));
  }
}
