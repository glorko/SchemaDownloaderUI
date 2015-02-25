package net.punklan.glorfindeil;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Item;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Table;
import schemacrawler.schema.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
@Theme("mytheme")
@Widgetset("net.punklan.glorfindeil.MyAppWidgetset")
public class SchemaCrawlerWebForm extends UI {
    private static Logger log = Logger.getLogger(SchemaCrawlerWebForm.class.getName());

    public Boolean getTabsheetAdded() {
        return tabsheetAdded;
    }

    public void setTabsheetAdded(Boolean tabsheetAdded) {
        this.tabsheetAdded = tabsheetAdded;
    }

    private Boolean tabsheetAdded = false;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        final VerticalLayout layout = new VerticalLayout();
        final HorizontalLayout inputData = new HorizontalLayout();
        inputData.setSpacing(true);
        layout.setSpacing(true);
        layout.setMargin(true);
        setContent(layout);

        //Components for input
        final TextField jdbcString = new TextField("jdbcString");
        final TextField driver = new TextField("driver");
        final TextField user = new TextField("user");
        final PasswordField password = new PasswordField("password");
        final Button button = new Button("get database scheme");

        inputData.addComponent(jdbcString);
        inputData.addComponent(driver);
        inputData.addComponent(user);
        inputData.addComponent(password);

        layout.addComponent(inputData);
        layout.addComponent(button);
        //Tabsheet for output
        final TabSheet tabsheet = new TabSheet();
        //variable used for not showing loading gif when tabsheet is empty


        final SchemaCrawlerHelper helper = new SchemaCrawlerHelper();
        // Handle the button click event
        button.addClickListener(new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
                if (!getTabsheetAdded()) {
                    layout.addComponent(tabsheet);
                    setTabsheetAdded(true);
                }
                //Create tree
                final Tree tree = new Tree("Scheme->Table->Column");
                tree.addContainerProperty("CrawlerObject", NamedObjectWithAttributes.class, null);
                //Create table for attributes
                final Table attributes = new Table("Attributes");
                attributes.addContainerProperty("Attribute", String.class, null);
                attributes.addContainerProperty("String value", String.class, null);
                //Create layouts in tab
                VerticalLayout tab = new VerticalLayout();
                HorizontalSplitPanel inTab = new HorizontalSplitPanel();
                tab.setSpacing(true);

                Connection conn = null;
                //If we got exception on parsing or retrieving schema then we get error on screen
                try {
                    conn = helper.getConnection(driver.getValue(), jdbcString.getValue(), user.getValue(), password.getValue());
                    Catalog catalog = helper.getCatalogForConnection(conn);
                    //put objects in the tree
                    Object root = tree.addItem("Database");
                    for (final Schema schema : catalog.getSchemas()) {

                        tree.addItem(schema);
                        Item schemaItem = tree.getItem(schema);
                        schemaItem.getItemProperty("CrawlerObject").setValue(schema);
                        log.log(Level.INFO, "o--> " + schema);
                        tree.setParent(schema, "Database");
                        for (final schemacrawler.schema.Table table : catalog.getTables(schema)) {
                            String tableKey = table.getName();
                            if(tree.containsId(tableKey)){
                                tableKey = table+"";
                            }
                            tree.addItem(tableKey);
                            tree.setParent(tableKey, schema);
                            Item tableItem = tree.getItem(tableKey);
                            tableItem.getItemProperty("CrawlerObject").setValue(table);

                            log.log(Level.INFO, "   o--> " + table);
                            for (final Column column : table.getColumns()) {
                                //Name makes as nameOfColumn:datataype:PK( if PK)
                                String key = column.getName() + ":" + column.getColumnDataType();
                                if (column.isPartOfPrimaryKey()) {
                                    key = key + ":PK";
                                }
                                if(tree.containsId(key)){
                                    key = table+ ":" + column.getColumnDataType();
                                    if (column.isPartOfPrimaryKey()) {
                                        key = key + ":PK";
                                    }
                                }

                                tree.addItem(key);
                                tree.setParent(key, tableKey);
                                Item columnItem = tree.getItem(key);
                                tree.setChildrenAllowed(key, false);
                                columnItem.getItemProperty("CrawlerObject").setValue(column);
                                log.log(Level.INFO, "           o--> " + key);

                            }
                        }

                    }
                    //Listener on element choose
                    tree.addItemClickListener(new ItemClickEvent.ItemClickListener() {
                        @Override
                        public void itemClick(ItemClickEvent itemClickEvent) {
                            Object itemId = itemClickEvent.getItemId();
                            Notification.show(itemId + "", Notification.Type.HUMANIZED_MESSAGE);
                            //If item contains SchemaCrawler object with attributes, put them in the table
                            if (tree.getItem(itemId).getItemProperty("CrawlerObject").getValue() != null) {
                                NamedObjectWithAttributes object = (NamedObjectWithAttributes) tree.getItem(itemId).getItemProperty("CrawlerObject").getValue();
                                attributes.removeAllItems();
                                for (String key : object.getAttributes().keySet()) {
                                    Object newItemId = attributes.addItem();
                                    Item row1 = attributes.getItem(newItemId);
                                    row1.getItemProperty("Attribute").setValue(key);
                                    row1.getItemProperty("String value").setValue(object.getAttributes().get(key) + "");
                                }
                            }
                            attributes.setPageLength(attributes.size());
                        }
                    });
                    //add components and make new tab in tabsheet
                    inTab.setFirstComponent(tree);
                    inTab.setSecondComponent(attributes);
                    tab.addComponent(inTab);
                    tabsheet.addTab(tab, jdbcString.getValue());
                } catch (Exception e) {
                    log.log(Level.WARNING, "Error on retrieving " + jdbcString.getValue(), e);
                    Notification.show("Error on schema reading",
                            e.getMessage(),
                            Notification.Type.ERROR_MESSAGE);
                } finally {
                    //close connection if opened
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = SchemaCrawlerWebForm.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    }
}
