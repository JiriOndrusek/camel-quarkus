/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package org.apache.camel.quarkus.component.avro.rpc.it.specific.generated;

import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.SchemaStore;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;

@org.apache.avro.specific.AvroGenerated
public class Key extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
    private static final long serialVersionUID = -1622555054919462235L;
    public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse(
            "{\"type\":\"record\",\"name\":\"Key\",\"namespace\":\"org.apache.camel.quarkus.component.avro.rpc.it.specific.generated\",\"fields\":[{\"name\":\"key\",\"type\":\"string\"}]}");

    public static org.apache.avro.Schema getClassSchema() {
        return SCHEMA$;
    }

    private static SpecificData MODEL$ = new SpecificData();

    private static final BinaryMessageEncoder<Key> ENCODER = new BinaryMessageEncoder<Key>(MODEL$, SCHEMA$);

    private static final BinaryMessageDecoder<Key> DECODER = new BinaryMessageDecoder<Key>(MODEL$, SCHEMA$);

    /**
     * Return the BinaryMessageEncoder instance used by this class.
     * 
     * @return the message encoder used by this class
     */
    public static BinaryMessageEncoder<Key> getEncoder() {
        return ENCODER;
    }

    /**
     * Return the BinaryMessageDecoder instance used by this class.
     * 
     * @return the message decoder used by this class
     */
    public static BinaryMessageDecoder<Key> getDecoder() {
        return DECODER;
    }

    /**
     * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
     * 
     * @param  resolver a {@link SchemaStore} used to find schemas by fingerprint
     * @return          a BinaryMessageDecoder instance for this class backed by the given SchemaStore
     */
    public static BinaryMessageDecoder<Key> createDecoder(SchemaStore resolver) {
        return new BinaryMessageDecoder<Key>(MODEL$, SCHEMA$, resolver);
    }

    /**
     * Serializes this Key to a ByteBuffer.
     * 
     * @return                     a buffer holding the serialized data for this instance
     * @throws java.io.IOException if this instance could not be serialized
     */
    public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
        return ENCODER.encode(this);
    }

    /**
     * Deserializes a Key from a ByteBuffer.
     * 
     * @param  b                   a byte buffer holding serialized data for an instance of this class
     * @return                     a Key instance decoded from the given buffer
     * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
     */
    public static Key fromByteBuffer(
            java.nio.ByteBuffer b) throws java.io.IOException {
        return DECODER.decode(b);
    }

    @Deprecated
    public java.lang.CharSequence key;

    /**
     * Default constructor. Note that this does not initialize fields
     * to their default values from the schema. If that is desired then
     * one should use <code>newBuilder()</code>.
     */
    public Key() {
    }

    /**
     * All-args constructor.
     * 
     * @param key The new value for key
     */
    public Key(java.lang.CharSequence key) {
        this.key = key;
    }

    public org.apache.avro.specific.SpecificData getSpecificData() {
        return MODEL$;
    }

    public org.apache.avro.Schema getSchema() {
        return SCHEMA$;
    }

    // Used by DatumWriter.  Applications should not call.
    public java.lang.Object get(int field$) {
        switch (field$) {
        case 0:
            return key;
        default:
            throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    // Used by DatumReader.  Applications should not call.
    @SuppressWarnings(value = "unchecked")
    public void put(int field$, java.lang.Object value$) {
        switch (field$) {
        case 0:
            key = (java.lang.CharSequence) value$;
            break;
        default:
            throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    /**
     * Gets the value of the 'key' field.
     * 
     * @return The value of the 'key' field.
     */
    public java.lang.CharSequence getKey() {
        return key;
    }

    /**
     * Sets the value of the 'key' field.
     * 
     * @param value the value to set.
     */
    public void setKey(java.lang.CharSequence value) {
        this.key = value;
    }

    /**
     * Creates a new Key RecordBuilder.
     * 
     * @return A new Key RecordBuilder
     */
    public static org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Key.Builder newBuilder() {
        return new org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Key.Builder();
    }

    /**
     * Creates a new Key RecordBuilder by copying an existing Builder.
     * 
     * @param  other The existing builder to copy.
     * @return       A new Key RecordBuilder
     */
    public static org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Key.Builder newBuilder(
            org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Key.Builder other) {
        if (other == null) {
            return new org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Key.Builder();
        } else {
            return new org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Key.Builder(other);
        }
    }

    /**
     * Creates a new Key RecordBuilder by copying an existing Key instance.
     * 
     * @param  other The existing instance to copy.
     * @return       A new Key RecordBuilder
     */
    public static org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Key.Builder newBuilder(
            org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Key other) {
        if (other == null) {
            return new org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Key.Builder();
        } else {
            return new org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Key.Builder(other);
        }
    }

    /**
     * RecordBuilder for Key instances.
     */
    public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Key>
            implements org.apache.avro.data.RecordBuilder<Key> {

        private java.lang.CharSequence key;

        /** Creates a new Builder */
        private Builder() {
            super(SCHEMA$);
        }

        /**
         * Creates a Builder by copying an existing Builder.
         * 
         * @param other The existing Builder to copy.
         */
        private Builder(org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Key.Builder other) {
            super(other);
            if (isValidValue(fields()[0], other.key)) {
                this.key = data().deepCopy(fields()[0].schema(), other.key);
                fieldSetFlags()[0] = other.fieldSetFlags()[0];
            }
        }

        /**
         * Creates a Builder by copying an existing Key instance
         * 
         * @param other The existing instance to copy.
         */
        private Builder(org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Key other) {
            super(SCHEMA$);
            if (isValidValue(fields()[0], other.key)) {
                this.key = data().deepCopy(fields()[0].schema(), other.key);
                fieldSetFlags()[0] = true;
            }
        }

        /**
         * Gets the value of the 'key' field.
         * 
         * @return The value.
         */
        public java.lang.CharSequence getKey() {
            return key;
        }

        /**
         * Sets the value of the 'key' field.
         * 
         * @param  value The value of 'key'.
         * @return       This builder.
         */
        public org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Key.Builder setKey(
                java.lang.CharSequence value) {
            validate(fields()[0], value);
            this.key = value;
            fieldSetFlags()[0] = true;
            return this;
        }

        /**
         * Checks whether the 'key' field has been set.
         * 
         * @return True if the 'key' field has been set, false otherwise.
         */
        public boolean hasKey() {
            return fieldSetFlags()[0];
        }

        /**
         * Clears the value of the 'key' field.
         * 
         * @return This builder.
         */
        public org.apache.camel.quarkus.component.avro.rpc.it.specific.generated.Key.Builder clearKey() {
            key = null;
            fieldSetFlags()[0] = false;
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Key build() {
            try {
                Key record = new Key();
                record.key = fieldSetFlags()[0] ? this.key : (java.lang.CharSequence) defaultValue(fields()[0]);
                return record;
            } catch (org.apache.avro.AvroMissingFieldException e) {
                throw e;
            } catch (java.lang.Exception e) {
                throw new org.apache.avro.AvroRuntimeException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static final org.apache.avro.io.DatumWriter<Key> WRITER$ = (org.apache.avro.io.DatumWriter<Key>) MODEL$
            .createDatumWriter(SCHEMA$);

    @Override
    public void writeExternal(java.io.ObjectOutput out)
            throws java.io.IOException {
        WRITER$.write(this, SpecificData.getEncoder(out));
    }

    @SuppressWarnings("unchecked")
    private static final org.apache.avro.io.DatumReader<Key> READER$ = (org.apache.avro.io.DatumReader<Key>) MODEL$
            .createDatumReader(SCHEMA$);

    @Override
    public void readExternal(java.io.ObjectInput in)
            throws java.io.IOException {
        READER$.read(this, SpecificData.getDecoder(in));
    }

    @Override
    protected boolean hasCustomCoders() {
        return true;
    }

    @Override
    public void customEncode(org.apache.avro.io.Encoder out)
            throws java.io.IOException {
        out.writeString(this.key);

    }

    @Override
    public void customDecode(org.apache.avro.io.ResolvingDecoder in)
            throws java.io.IOException {
        org.apache.avro.Schema.Field[] fieldOrder = in.readFieldOrderIfDiff();
        if (fieldOrder == null) {
            this.key = in.readString(this.key instanceof Utf8 ? (Utf8) this.key : null);

        } else {
            for (int i = 0; i < 1; i++) {
                switch (fieldOrder[i].pos()) {
                case 0:
                    this.key = in.readString(this.key instanceof Utf8 ? (Utf8) this.key : null);
                    break;

                default:
                    throw new java.io.IOException("Corrupt ResolvingDecoder.");
                }
            }
        }
    }
}
