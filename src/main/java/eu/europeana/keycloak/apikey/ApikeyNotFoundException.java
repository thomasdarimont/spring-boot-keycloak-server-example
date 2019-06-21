package eu.europeana.keycloak.apikey;

class ApikeyNotFoundException extends Exception {
    ApikeyNotFoundException(String apikey) {
        super("Apikey " + apikey + " not found.");
    }
}
