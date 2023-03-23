package com.ibm.icu.text;

//todo add condition and remove this class if proper jar is on classpath by RemovedResourceBuildItem
public class Normalizer {

    public static abstract class Mode {
        /**
         * Sole constructor
         * 
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        protected Mode() {
        }

        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        protected abstract Normalizer getNormalizer(int options);
    }

    private static final class NFCMode extends Mode {
        @Override
        protected Normalizer getNormalizer(int options) {
            return null;
        }
    }

    @Deprecated
    public static final Mode NFC = new NFCMode();

    public static int normalize(char[] message, char[] normalizationResult, Mode m, int i) {
        throw new RuntimeException("This is a proxy method, should be never called!");
    }

}
