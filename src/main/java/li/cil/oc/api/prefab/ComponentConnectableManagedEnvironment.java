package li.cil.oc.api.prefab;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Node;

public abstract class ComponentConnectableManagedEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    protected ComponentConnector node;

    @Override
    public Node node() {
        return this.node != null ? this.node : super.node();
    }

    @Override
    protected void setNode(Node value) {
        if (value == null) {
            this.node = null;
        } else if (value instanceof ComponentConnector connector) {
            this.node = connector;
        }
        super.setNode(value);
    }
}
