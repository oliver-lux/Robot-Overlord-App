package com.marginallyclever.ro3.texture;

import com.marginallyclever.ro3.Registry;
import com.marginallyclever.ro3.listwithevents.ItemAddedListener;
import com.marginallyclever.ro3.listwithevents.ItemRemovedListener;
import com.marginallyclever.ro3.mesh.MeshFactoryDialog;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.util.Objects;

/**
 * <p>The {@link TextureChooserDialog} class allows for selecting a {@link com.marginallyclever.ro3.texture.TextureWithMetadata}
 * that has been previously loaded by the {@link com.marginallyclever.ro3.texture.TextureFactory}.
 * This class also provides access to the {@link TextureFactoryDialog} for loading additional meshes.</p>
 * <p>TODO In the future it would be nice to count references and unload it when no longer needed.</p>
 */
public class TextureChooserDialog extends JPanel
        implements ItemAddedListener<TextureWithMetadata>, ItemRemovedListener<TextureWithMetadata> {
    private final DefaultListModel<TextureWithMetadata> model = new DefaultListModel<>();
    private final JList<TextureWithMetadata> list = new JList<>();
    private final JToolBar toolBar = new JToolBar();
    private TextureWithMetadata selectedItem;
    private String viewType;

    public TextureChooserDialog() {
        super(new BorderLayout());
        setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

        setupToolbar();
        setupList();

        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);
    }

    public void setSelectedItem(TextureWithMetadata mesh) {
        selectedItem = mesh;
        if (mesh == null) {
            list.clearSelection();
        } else {
            list.setSelectedValue(mesh, true);
        }
    }

    private void setupToolbar() {
        var loadButton = new JButton("Load");
        loadButton.setToolTipText("Load from a file.");
        loadButton.setIcon(new ImageIcon(Objects.requireNonNull(getClass().getResource(
                "/com/marginallyclever/ro3/apps/actions/icons8-load-16.png"))));
        loadButton.addActionListener(e-> runFactoryDialog((JComponent)e.getSource()));
        toolBar.add(loadButton);

        var clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> setSelectedItem(null));
        clearButton.setToolTipText("Choose none.");
        clearButton.setIcon(new ImageIcon(Objects.requireNonNull(getClass().getResource(
                "/com/marginallyclever/ro3/apps/icons8-reset-16.png"))));
        toolBar.add(clearButton);

        /*
        String[] viewTypes = {"List View", "Detail View", "Thumbnail View"};
        JComboBox<String> viewTypeComboBox = new JComboBox<>(viewTypes);
        viewTypeComboBox.addActionListener(e -> {
            viewType = (String) viewTypeComboBox.getSelectedItem();
            updateView();
        });
        toolbar.add(viewTypeComboBox);
        */
    }

    private void runFactoryDialog(JComponent parent) {
        var textureFactoryDialog = new TextureFactoryDialog();
        int result = textureFactoryDialog.run(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            TextureWithMetadata mesh = textureFactoryDialog.getTexture();
            setSelectedItem(mesh);
        }
    }

    private void setupList() {
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                TextureWithMetadata item = (TextureWithMetadata) value;
                String text = item.getSource() + " (" + item.getWidth()+"x"+item.getHeight()+")";
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });

        for (TextureWithMetadata mesh : Registry.textureFactory.getPool().getList()) {
            model.addElement(mesh);
        }
        list.setModel(model);
        list.setSelectedValue(selectedItem, true);
        list.addListSelectionListener(e -> selectedItem = list.getSelectedValue());
    }

    @Override
    public void addNotify() {
        super.addNotify();
        Registry.textureFactory.getPool().addItemAddedListener(this);
        Registry.textureFactory.getPool().addItemRemovedListener(this);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        Registry.textureFactory.getPool().removeItemAddedListener(this);
        Registry.textureFactory.getPool().removeItemRemovedListener(this);
    }

    private void updateView() {
        // Update the list of meshes based on the selected view type
        // This could involve changing the ListCellRenderer of the JList
        // For example, for the "Thumbnail View", you could display a small preview of each mesh
    }

    public TextureWithMetadata getSelectedItem() {
        return selectedItem;
    }

    @Override
    public void itemAdded(Object source, TextureWithMetadata item) {
        model.addElement(item);
    }

    @Override
    public void itemRemoved(Object source, TextureWithMetadata item) {
        model.removeElement(item);
    }

    /**
     * Run the selection as a dialog.
     * @param parent the parent component for the dialog.
     * @return JFileChooser.APPROVE_OPTION or JFileChooser.CANCEL_OPTION.  return type is int because this is a
     * JFileChooser replacement.  It is consistent with {@link MeshFactoryDialog} and {@link TextureFactoryDialog}.
     */
    public int run(JComponent parent) {
        int result = JOptionPane.showOptionDialog(
                parent,
                this,
                "Select",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                null);
        if(result == JOptionPane.OK_OPTION) {
            return JFileChooser.APPROVE_OPTION;
        }
        return JFileChooser.CANCEL_OPTION;
    }
}
