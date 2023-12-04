package com.mediamelon.smartstreaming;

public class MMRegistrationInformation{
    /*
     * Creates the object to have information identifying the customer, subscriber, and player to which integration is done
     */
    public MMRegistrationInformation(String aCustomerId, String aPlayerName){
        customerID = aCustomerId;
        component = "ANDROIDSDK";
        playerName = aPlayerName;
    }

    /*
     * Some business organizations may would like to do analytics segmented by group.
     * For example, a Media House may have many divisions, and will like to categorize their analysis
     * based on division. Or a content owner has distributed content to various resellers and would
     * like to know the reseller from whom the user is playing the content.
     * In this case every reseller will have separate application, and will configure the domain name.
     *
     * Call to this API is optional
     */
    public void setDomain(String domain){
        domainName = domain;
    }

    /*
     * Provides the subscriber information to the SDK.
     * Subscriber information includes identifier identifying the subscriber (genrally email id, or UUID of app installation etc.),
     * Its type - For example Premium, Basic etc (Integrators can choose any value for type depending on the damain of business in
     * which player is used. From perspective of Smartsight, it is opaque data, and is not interpreted in any way by it.
     * Tag - Additional metadata corresponding to the asset. From perspective of Smartsight, no meaning is attached to it, and it is
     * reflect as is.
     *
     * Call to this API is optional
     */
    public void setSubscriberInformation(String subsID, String subsType, String subsTag){
        subscriberID = subsID;
        subscriberType = subsType;
        subscriberTag = subsTag;
    }

    /**
     * Sets the player information. Please note that brand, model and version mentioned here are with respect to player and not wrt device
     * i.e. Even though brand for device is Apple, but brand here could be the brand, that integrator want to assign to this player.
     * For example - It could be the name of Media Vendor.
     * Model - This could be used to further classify the player, for example XYZ framework based player, or VOD player or Live player etc
     * Version - This is used to indicate the version of the player
     * All these params are optionals and you may set them to nil
     *
     * Call to this API is optional
     */
    public void setPlayerInformation(String brand, String model, String version){
        playerBrand = brand;
        playerModel = model;
        playerVersion = version;
    }

    public String customerID;
    public String component;

    public String playerName;
    public String domainName;

    public String subscriberID;
    public String subscriberType;
    public String subscriberTag;

    public String playerBrand;
    public String playerModel;
    public String playerVersion;
}
