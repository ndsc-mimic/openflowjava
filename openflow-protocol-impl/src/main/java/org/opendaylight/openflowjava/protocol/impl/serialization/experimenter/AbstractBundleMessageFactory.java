/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.openflowjava.protocol.impl.serialization.experimenter;

import io.netty.buffer.ByteBuf;
import java.util.List;
import org.opendaylight.openflowjava.protocol.api.extensibility.OFSerializer;
import org.opendaylight.openflowjava.protocol.api.extensibility.SerializerRegistry;
import org.opendaylight.openflowjava.protocol.api.extensibility.SerializerRegistryInjector;
import org.opendaylight.openflowjava.protocol.api.util.EncodeConstants;
import org.opendaylight.openflowjava.util.ByteBufUtils;
import org.opendaylight.openflowjava.util.ExperimenterSerializerKeyFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.approved.extensions.rev160802.BundleFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.approved.extensions.rev160802.BundlePropertyType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.approved.extensions.rev160802.bundle.properties.BundleProperty;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.approved.extensions.rev160802.bundle.properties.bundle.property.bundle.property.entry.BundleExperimenterProperty;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.approved.extensions.rev160802.bundle.properties.bundle.property.bundle.property.entry.bundle.experimenter.property.BundleExperimenterPropertyData;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for common stuff of bundle messages.
 */
public abstract class AbstractBundleMessageFactory<T extends DataContainer> implements OFSerializer<T>,
        SerializerRegistryInjector {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBundleMessageFactory.class);
    protected SerializerRegistry serializerRegistry;

    @Override
    public void serialize(T input, ByteBuf outBuffer) {
        // to be extended
    }

    @Override
    public void injectSerializerRegistry(SerializerRegistry serializerRegistry) {
        this.serializerRegistry = serializerRegistry;
    }

    protected static void writeBundleFlags(final BundleFlags bundleFlags, final ByteBuf outBuffer) {
        int flagsBitMap = ByteBufUtils.fillBitMask(0, bundleFlags.isAtomic(), bundleFlags.isOrdered());
        outBuffer.writeShort(flagsBitMap);
    }

    protected void writeBundleProperties(final List<BundleProperty> properties, final ByteBuf outBuffer) {
        for (BundleProperty property : properties) {
            BundlePropertyType type = property.getType();
            if (type != null && type.equals(BundlePropertyType.ONFETBPTEXPERIMENTER)) {
                int startIndex = outBuffer.writerIndex();
                outBuffer.writeShort(type.getIntValue());
                int lengthIndex = outBuffer.writerIndex();
                outBuffer.writeShort(EncodeConstants.EMPTY_LENGTH);
                writeBundleExperimenterProperty(property, outBuffer);
                outBuffer.setShort(lengthIndex, outBuffer.writerIndex() - startIndex);
            } else {
                LOG.warn("lTrying to serialize unknown bundle property (type: {}), skipping", type.getIntValue() );
            }
        }
    }

    protected void writeBundleExperimenterProperty(final BundleProperty bundleProperty, final ByteBuf outBuffer) {
        BundleExperimenterProperty property = (BundleExperimenterProperty) bundleProperty.getBundlePropertyEntry();
        int experimenterId = property.getExperimenter().getValue().intValue();
        int expType = property.getExpType().intValue();
        outBuffer.writeInt(experimenterId);
        outBuffer.writeInt(expType);
        OFSerializer<BundleExperimenterPropertyData> serializer = serializerRegistry.getSerializer(
                ExperimenterSerializerKeyFactory.createBundlePropertySerializerKey(EncodeConstants.OF13_VERSION_ID,
                        experimenterId, expType));
        serializer.serialize(property.getBundleExperimenterPropertyData(), outBuffer);
    }

}
