import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

class SelectableTreeCellRenderer extends DefaultTreeCellRenderer {

    private JCheckBox selected;
    private JPanel renderComponent;

    public SelectableTreeCellRenderer() {
        selected = new JCheckBox();
        renderComponent = new JPanel(new BorderLayout());
        renderComponent.add(selected,BorderLayout.WEST);

        selected.setOpaque(false);
        renderComponent.setOpaque(false);
    }

    public Component getTreeCellRendererComponent(
        JTree tree,
        Object value,
        boolean sel,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus) {

        Component c = super.getTreeCellRendererComponent(
            tree,
            value,
            false, // we pass 'false' rather than 'sel'
            expanded,
            leaf,
            row,
            hasFocus);

        selected.setSelected(sel);
        renderComponent.add(c,BorderLayout.CENTER);

        return renderComponent;
    }
}