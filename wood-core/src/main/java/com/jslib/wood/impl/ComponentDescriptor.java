package com.jslib.wood.impl;

import com.jslib.wood.FilePath;
import com.jslib.wood.IReferenceHandler;
import com.jslib.wood.SourceReader;

/**
 * Component descriptor contains properties customizable at component level. This is in contrast with {@link ProjectDescriptor}
 * that affects all components from project. Current implementation is actually used for pages; support for ordinary components
 * is expected.
 * <p>
 * Here are current implemented values:
 * <table border="1" style="border-collapse:collapse;">
 * <tr>
 * <td><b>Element
 * <td><b>Description
 * <td><b>Usage
 * <td><b>Sample Value
 * <tr>
 * <td>version
 * <td>component version especially useful for library components
 * <td>library logic does not use it but developer may want to know it
 * <td>1.2.3
 * <tr>
 * <td>title
 * <td>component title used to identify component on user interfaces
 * <td>current implementation uses title for page head <code>title</code> element
 * <td>Index Page
 * <tr>
 * <td>description
 * <td>component description is a concise explanation of the component content
 * <td>current implementation insert description into page head using <code>meta</code> element
 * <td>Index page description.
 * <tr>
 * <td>path
 * <td>directories path to store page layout into; build file system insert this directories path just before layout file
 * <td>usable for role based security supplied by servlet container
 * <td>/admin/
 * <tr>
 * <td>scripts
 * <td>contains path to third party scripts specific to component; both project file path and absolute URL are accepted
 * <td>scripts are included into page document in the defined order
 * <td>https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js
 * </table>
 * <p>
 * For convenience below is a sample configuration file. Element values can be replaced with string variables. For example
 * <code>title</code> value can be something like <code>@string/page-title</code>. This class uses {@link ReferencesResolver} to
 * replace variables with their defined values.
 * <p>
 * Third party scripts are usually linked at the page bottom, just before closing page body. Anyway, <code>script</code> element
 * has an optional boolean attribute, <code>append-to-head</code> to force including script link at the end of the page header.
 *
 * <pre>
 *  &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 *  &lt;page&gt;
 *      &lt;version&gt;1.2.3&lt;/version&gt;
 *      &lt;title&gt;Index Page&lt;/title&gt;
 *      &lt;description&gt;Index page description.&lt;/description&gt;
 *      &lt;path&gt;/admin/&lt;/path&gt;
 *      &lt;scripts&gt;
 *          &lt;script&gt;https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js&lt;/script&gt;
 *          &lt;script&gt;http://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js&lt;/script&gt;
 *      &lt;/scripts&gt;
 *  &lt;/page&gt;
 * </pre>
 *
 * <p>
 * The <code>path</code> element defines the directory path where page layout files are stored. By default, these files
 * are placed in a directory determined by the build file system implementation — for instance, the default file system
 * stores them in the build root.
 * However, if a <code>path</code> element is specified, the provided path will override the default location. For example,
 * in the following case, the <code>user-manager.htm</code> page is saved in the <code>admin</code> directory.
 * This feature is particularly useful for role-based security, as it enables grouping pages under specific paths, allowing
 * security constraints to be configured based on directory structure.
 *
 * <pre>
 *  /
 *  /admin/
 *        +-user-manager.htm
 *        ~
 *  /info/
 *        +-log.viewer.htm
 *        ~
 *  /media/
 *  /script/
 *  /style/
 *  +-login.htm
 *  ~
 * </pre>
 * <p>
 * Component descriptor instance has no mutable state, therefore is thread safe.
 *
 * @author Iulian Rotaru
 * @version final
 * @see ProjectDescriptor
 */
public class ComponentDescriptor extends BaseDescriptor {
    /**
     * File path for component descriptor. It has owning component name and XML extension.
     */
    private final FilePath descriptorFile;

    /**
     * Create component descriptor instance and initialize it from given file. Values defined by descriptor may contain resource
     * references that need to be resolved. This descriptor uses external defined references handler just for that.
     *
     * @param descriptorFile   descriptor file path,
     * @param referenceHandler resource references handler.
     */
    public ComponentDescriptor(FilePath descriptorFile, IReferenceHandler referenceHandler) {
        super(descriptorFile, descriptorFile.exists() ? new SourceReader(descriptorFile, referenceHandler) : null);
        this.descriptorFile = descriptorFile;
    }

    public FilePath getDescriptorFile() {
        return descriptorFile;
    }

    /**
     * Get component description or null if description is missing. This property is loaded from <code>description</code>
     * element.
     *
     * @return object description, possible null.
     */
    public String getDescription() {
        return text("description");
    }

    /**
     * Get resources group on which this component belongs or null if component is in global space. Resources group is declared
     * in descriptor using <code>group</code> element.
     * <p>
     * Components can be declared on named resources groups. Build tool uses this group name as context path. For example, login
     * page can be declared on <code>WEB-INF</code> group so that it becomes private.
     *
     * @return resources group or null if component is in global space.
     */
    public String getResourcesGroup() {
        return text("group");
    }
}
