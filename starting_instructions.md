 Query Provider Services at Runtime                                                                                                                                                     ─
                                                                                                                                                                                                                                    
  The BouncyCastlePQCProvider registers all algorithms as services. You can query them:                                                                                                                                             
                                                        
  Provider provider = new BouncyCastlePQCProvider();                                                                                                                                                                                
  List<String> algorithms = provider.getServices().stream()                                                                                                                                                                         
      .filter(s -> Arrays.asList("Signature", "KeyPairGenerator", "Cipher", "KeyFactory")                                                                                                                                           
                         .contains(s.getType()))                                                                                                                                                                                    
      .map(Provider.Service::getAlgorithm)                                                                                                                                                                                          
      .distinct()                                                            PqcProcessor                                                                                                                                                       
      .collect(Collectors.toList());                                                                                                                                                                                                
                                                                                                                                                                                                                                    
  This would discover ALL algorithms including variants like:                                                                                                                                                                       
  - DILITHIUM, DILITHIUM2, DILITHIUM3, DILITHIUM5                                                                                                                                                                                   
  - ML-KEM, ML-KEM-512, ML-KEM-768, ML-KEM-1024                                                                                                                                                                                     
  - KYBER, FALCON, SPHINCSPLUS, etc.                    
                                                                                                                                                                                                                                    
  Advantages:                                                                                                                                                                                                                       
  - Automatic discovery                                                                                                                                                                                                             
  - Gets all variants                                                                                                                                                                                                               
  - Future-proof when Bouncy Castle updates                                                                                                                                                                                         
  - Simple implementation      
  
  use this ynamic load instead of static list in PqcProcessor
