package com.amazon.ata.advertising.service.businesslogic;

import com.amazon.ata.advertising.service.dao.ReadableDao;
import com.amazon.ata.advertising.service.model.*;
import com.amazon.ata.advertising.service.targeting.TargetingEvaluator;
import com.amazon.ata.advertising.service.targeting.TargetingGroup;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * This class is responsible for picking the advertisement to be rendered.
 */
public class AdvertisementSelectionLogic {

    private static final Logger LOG = LogManager.getLogger(AdvertisementSelectionLogic.class);

    private final ReadableDao<String, List<AdvertisementContent>> contentDao;
    private final ReadableDao<String, List<TargetingGroup>> targetingGroupDao;
    private Random random = new Random();

    /**
     * Constructor for AdvertisementSelectionLogic.
     * @param contentDao Source of advertising content.
     * @param targetingGroupDao Source of targeting groups for each advertising content.
     */
    @Inject
    public AdvertisementSelectionLogic(ReadableDao<String, List<AdvertisementContent>> contentDao,
                                       ReadableDao<String, List<TargetingGroup>> targetingGroupDao) {
        this.contentDao = contentDao;
        this.targetingGroupDao = targetingGroupDao;
    }

    /**
     * Setter for Random class.
     * @param random generates random number used to select advertisements.
     */
    public void setRandom(Random random) {
        this.random = random;
    }

    /**
     * Gets all of the content and metadata for the marketplace and determines which content can be shown.  Returns the
     * eligible content with the highest click through rate.  If no advertisement is available or eligible, returns an
     * EmptyGeneratedAdvertisement.
     *
     * @param customerId - the customer to generate a custom advertisement for
     * @param marketplaceId - the id of the marketplace the advertisement will be rendered on
     * @return an advertisement customized for the customer id provided, or an empty advertisement if one could
     *     not be generated.
     */
//    public GeneratedAdvertisement selectAdvertisement(String customerId, String marketplaceId) {
//
////        GeneratedAdvertisement generatedAdvertisement = new EmptyGeneratedAdvertisement();
////        if (StringUtils.isEmpty(marketplaceId)) {
////            LOG.warn("MarketplaceId cannot be null or empty. Returning empty ad.");
////        } else {
////            final List<AdvertisementContent> contents = contentDao.get(marketplaceId);
////
////            if (CollectionUtils.isNotEmpty(contents)) {
////                AdvertisementContent randomAdvertisementContent = contents.get(random.nextInt(contents.size()));
////                generatedAdvertisement = new GeneratedAdvertisement(randomAdvertisementContent);
////            }
////        }

//        if (StringUtils.isEmpty(marketplaceId)) {
//            LOG.warn("MarketplaceId cannot be null or empty. Returning empty ad.");
//            return new EmptyGeneratedAdvertisement();
//        }
//        List<AdvertisementContent> contents = contentDao.get(marketplaceId);
//
////        List<AdvertisementContent> eligibleAds = new ArrayList<>();
//        TargetingEvaluator targetingEvaluator = new TargetingEvaluator(new RequestContext(customerId, marketplaceId));
//
//        List<AdvertisementContent> eligibleAds = new ArrayList<>(contents.stream().filter(content->{
//
//            if(content.getContentId()!= null){
//                List<TargetingGroup> targetingGroups = targetingGroupDao.get(content.getContentId());
//                for(TargetingGroup targetingGroup: targetingGroups){
//                    if(targetingEvaluator.evaluate(targetingGroup).isTrue()){
//                        return true;
//                    }
//                }
//            }
//            return false;
//        }).collect(Collectors.toList()));
//
////        if (contents != null) {
////                    for(TargetingGroup targetingGroup : targetingGroups) {
////                        System.out.println("Hello");
////                                eligibleAds.addAll(contents.stream()
////                                .filter(ad -> targetingEvaluator.evaluate(targetingGroup).isTrue())
////                                .collect(Collectors.toList()));
////                    }
////        }
//        System.out.println(eligibleAds.size());
//
//        if (eligibleAds.size() == 0) {
//            return new EmptyGeneratedAdvertisement();
//        } else {
//            AdvertisementContent ad = eligibleAds.get(random.nextInt(eligibleAds.size()));
//            return new GeneratedAdvertisement(ad);
//        }
//    }
    public GeneratedAdvertisement selectAdvertisement(String customerId, String marketplaceId) {

        if (StringUtils.isEmpty(marketplaceId)) {
            LOG.warn("MarketplaceId cannot be null or empty. Returning empty ad.");
            return new EmptyGeneratedAdvertisement();
        }

        List<AdvertisementContent> contents = contentDao.get(marketplaceId);

        TargetingEvaluator targetingEvaluator = new TargetingEvaluator(new RequestContext(customerId, marketplaceId));

        TreeMap<Double, AdvertisementContent> eligibleAds = new TreeMap<>(Comparator.reverseOrder());

        for (AdvertisementContent content : contents) {
            if (content.getContentId() != null) {
                List<TargetingGroup> targetingGroups = targetingGroupDao.get(content.getContentId());
                for (TargetingGroup targetingGroup : targetingGroups) {
                    if (targetingEvaluator.evaluate(targetingGroup).isTrue()) {
                        double clickThroughRate = targetingGroup.getClickThroughRate();
                        eligibleAds.put(clickThroughRate, content);
                        break;
                    }
                }
            }
        }

        if (eligibleAds.size() == 0) {
            return new EmptyGeneratedAdvertisement();
        } else {
            AdvertisementContent ad = eligibleAds.firstEntry().getValue();
            return new GeneratedAdvertisement(ad);
        }
    }
}
