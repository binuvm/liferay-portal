/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.change.tracking.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.change.tracking.CTManager;
import com.liferay.change.tracking.constants.CTConstants;
import com.liferay.change.tracking.constants.CTPortletKeys;
import com.liferay.change.tracking.model.CTCollection;
import com.liferay.change.tracking.model.CTEntry;
import com.liferay.change.tracking.service.CTCollectionLocalService;
import com.liferay.change.tracking.service.CTEntryLocalService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.portlet.PortalPreferences;
import com.liferay.portal.kernel.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.service.test.ServiceTestUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Daniel Kocsis
 */
@RunWith(Arquillian.class)
public class ChangeTrackingManagerTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		ServiceTestUtil.setUser(TestPropsValues.getUser());

		_user = UserTestUtil.addUser();
	}

	@After
	public void tearDown() throws Exception {
		_ctManager.disableChangeTracking(TestPropsValues.getUserId());
	}

	@Test
	public void testCheckoutCTCollection() throws Exception {
		_ctManager.enableChangeTracking(TestPropsValues.getUserId());

		CTCollection ctCollection = _ctCollectionLocalService.addCTCollection(
			TestPropsValues.getUserId(), "Test Change Tracking Collection",
			StringPool.BLANK, new ServiceContext());

		PortalPreferences portalPreferences =
			PortletPreferencesFactoryUtil.getPortalPreferences(
				_user.getUserId(), !_user.isDefaultUser());

		String recentCTCollectionId = portalPreferences.getValue(
			CTPortletKeys.CHANGE_LISTS, "recentCTCollectionId");

		Assert.assertNull(
			"Users's recent change tracking collection must be null",
			recentCTCollectionId);

		_ctManager.checkoutCTCollection(
			_user.getUserId(), ctCollection.getCtCollectionId());

		portalPreferences = PortletPreferencesFactoryUtil.getPortalPreferences(
			_user.getUserId(), !_user.isDefaultUser());

		recentCTCollectionId = portalPreferences.getValue(
			CTPortletKeys.CHANGE_LISTS, "recentCTCollectionId");

		Assert.assertEquals(
			"Users's recent change tracking collection must be properly set",
			GetterUtil.getLong(recentCTCollectionId),
			ctCollection.getCtCollectionId());
	}

	@Test
	public void testCheckoutCTCollectionWhenChangeTrackingIsDisabled()
		throws Exception {

		Assert.assertFalse(
			_ctManager.isChangeTrackingEnabled(TestPropsValues.getCompanyId()));

		PortalPreferences portalPreferences =
			PortletPreferencesFactoryUtil.getPortalPreferences(
				_user.getUserId(), !_user.isDefaultUser());

		String originalRecentCTCollectionId = portalPreferences.getValue(
			CTPortletKeys.CHANGE_LISTS, "recentCTCollectionId");

		CTCollection ctCollection = _ctCollectionLocalService.addCTCollection(
			TestPropsValues.getUserId(), "Test Change Tracking Collection",
			StringPool.BLANK, new ServiceContext());

		_ctManager.checkoutCTCollection(
			_user.getUserId(), ctCollection.getCtCollectionId());

		portalPreferences = PortletPreferencesFactoryUtil.getPortalPreferences(
			_user.getUserId(), !_user.isDefaultUser());

		String recentCTCollectionId = portalPreferences.getValue(
			CTPortletKeys.CHANGE_LISTS, "recentCTCollectionId");

		Assert.assertEquals(
			"Recent change tracking collection must not be changed",
			originalRecentCTCollectionId, recentCTCollectionId);
	}

	@Test
	public void testCreateCTCollection() throws Exception {
		_ctManager.enableChangeTracking(TestPropsValues.getUserId());

		final String name = RandomTestUtil.randomString();
		final String description = RandomTestUtil.randomString();

		Optional<CTCollection> ctCollectionOptional =
			_ctManager.createCTCollection(
				TestPropsValues.getUserId(), name, description);

		Assert.assertTrue(ctCollectionOptional.isPresent());

		CTCollection ctCollection = ctCollectionOptional.get();

		Assert.assertEquals(name, ctCollection.getName());
		Assert.assertEquals(description, ctCollection.getDescription());
	}

	@Test
	public void testCreateCTCollectionWhenChangeTrackingIsDisabled()
		throws Exception {

		Assert.assertFalse(
			_ctManager.isChangeTrackingEnabled(TestPropsValues.getCompanyId()));

		Optional<CTCollection> ctCollectionOptional =
			_ctManager.createCTCollection(
				TestPropsValues.getUserId(), RandomTestUtil.randomString(),
				RandomTestUtil.randomString());

		Assert.assertFalse(
			"Change tracking collection must be null",
			ctCollectionOptional.isPresent());
	}

	@Test
	public void testDeleteCTCollection() throws Exception {
		_ctManager.enableChangeTracking(TestPropsValues.getUserId());

		Optional<CTCollection> ctCollectionOptional =
			_ctManager.createCTCollection(
				TestPropsValues.getUserId(), RandomTestUtil.randomString(),
				RandomTestUtil.randomString());

		Assert.assertTrue(ctCollectionOptional.isPresent());

		CTCollection ctCollection = ctCollectionOptional.get();

		_ctManager.deleteCTCollection(ctCollection.getCtCollectionId());

		ctCollection = _ctCollectionLocalService.fetchCTCollection(
			ctCollection.getCtCollectionId());

		Assert.assertNull(
			"Change tracking collection must be null", ctCollection);
	}

	@Test
	public void testDeleteCTCollectionWhenInvalidCollectionId() {
		_ctManager.deleteCTCollection(RandomTestUtil.randomInt());
	}

	@Test
	public void testDisableChangeTracking() throws PortalException {
		_ctManager.enableChangeTracking(TestPropsValues.getUserId());

		List<CTCollection> ctCollections =
			_ctCollectionLocalService.getCTCollections(
				TestPropsValues.getCompanyId());

		Assert.assertEquals(
			"Change tracking collections must have one entry", 1,
			ctCollections.size());

		_ctManager.disableChangeTracking(TestPropsValues.getUserId());

		ctCollections = _ctCollectionLocalService.getCTCollections(
			TestPropsValues.getCompanyId());

		Assert.assertTrue(
			"Change tracking collection must not exist",
			ListUtil.isEmpty(ctCollections));
	}

	@Test
	public void testDisableChangeTrackingWhenChangeTrackingIsDisabled()
		throws PortalException {

		Assert.assertFalse(
			_ctManager.isChangeTrackingEnabled(TestPropsValues.getCompanyId()));

		_ctManager.disableChangeTracking(TestPropsValues.getUserId());

		Assert.assertFalse(
			_ctManager.isChangeTrackingEnabled(TestPropsValues.getCompanyId()));
	}

	@Test
	public void testEnableChangeTracking() throws PortalException {
		int ctCollectionsCount =
			_ctCollectionLocalService.getCTCollectionsCount();

		Assert.assertEquals(
			"Change tracking collection number must be zero", 0,
			ctCollectionsCount);

		_ctManager.enableChangeTracking(TestPropsValues.getUserId());

		List<CTCollection> ctCollections =
			_ctCollectionLocalService.getCTCollections(
				TestPropsValues.getCompanyId());

		Assert.assertEquals(
			"Change tracking collections must have one entry", 1,
			ctCollections.size());

		CTCollection ctCollection = ctCollections.get(0);

		Assert.assertEquals(
			CTConstants.CT_COLLECTION_NAME_PRODUCTION, ctCollection.getName());
	}

	@Test
	public void testEnableChangeTrackingWhenChangeTrackingIsEnabled()
		throws PortalException {

		_ctManager.enableChangeTracking(TestPropsValues.getUserId());

		Assert.assertTrue(
			_ctManager.isChangeTrackingEnabled(TestPropsValues.getCompanyId()));

		_ctManager.enableChangeTracking(TestPropsValues.getUserId());

		Assert.assertTrue(
			_ctManager.isChangeTrackingEnabled(TestPropsValues.getCompanyId()));
	}

	@Test
	public void testGetActiveCTCollectionOptional() throws Exception {
		_ctManager.enableChangeTracking(TestPropsValues.getUserId());

		Optional<CTCollection> ctCollectionOptional =
			_ctManager.createCTCollection(
				TestPropsValues.getUserId(), RandomTestUtil.randomString(),
				RandomTestUtil.randomString());

		PortalPreferences portalPreferences =
			PortletPreferencesFactoryUtil.getPortalPreferences(
				_user.getUserId(), !_user.isDefaultUser());

		portalPreferences.setValue(
			CTPortletKeys.CHANGE_LISTS, "recentCTCollectionId",
			String.valueOf(
				ctCollectionOptional.map(
					CTCollection::getCtCollectionId
				).orElse(
					0L
				)));

		Optional<CTCollection> activeCTCollectionOptional =
			_ctManager.getActiveCTCollectionOptional(_user.getUserId());

		Assert.assertTrue(activeCTCollectionOptional.isPresent());
		Assert.assertEquals(
			"Change tracking collections must be equal",
			ctCollectionOptional.get(), activeCTCollectionOptional.get());
	}

	@Test
	public void testGetActiveCTCollectionOptionalWhenChangeTrackingIsDisabled()
		throws Exception {

		CTCollection ctCollection = _ctCollectionLocalService.addCTCollection(
			TestPropsValues.getUserId(), RandomTestUtil.randomString(),
			RandomTestUtil.randomString(), new ServiceContext());

		PortalPreferences portalPreferences =
			PortletPreferencesFactoryUtil.getPortalPreferences(
				_user.getUserId(), !_user.isDefaultUser());

		portalPreferences.setValue(
			CTPortletKeys.CHANGE_LISTS, "recentCTCollectionId",
			String.valueOf(ctCollection.getCtCollectionId()));

		Optional<CTCollection> activeCTCollectionOptional =
			_ctManager.getActiveCTCollectionOptional(_user.getUserId());

		Assert.assertFalse(
			"Change tracking collection must be null",
			activeCTCollectionOptional.isPresent());
	}

	@Test
	public void testGetCTCollectionOptional() throws Exception {
		_ctManager.enableChangeTracking(TestPropsValues.getUserId());

		Optional<CTCollection> ctCollectionOptional1 =
			_ctManager.createCTCollection(
				TestPropsValues.getUserId(), RandomTestUtil.randomString(),
				RandomTestUtil.randomString());

		Assert.assertTrue(
			"Change tracking collection must not be null",
			ctCollectionOptional1.isPresent());

		CTCollection ctCollection = ctCollectionOptional1.get();

		Optional<CTCollection> ctCollectionOptional2 =
			_ctManager.getCTCollectionOptional(
				ctCollection.getCtCollectionId());

		Assert.assertEquals(
			"Change tracking collections must be equal", ctCollection,
			ctCollectionOptional2.get());
	}

	@Test
	public void testGetCTCollections() throws Exception {
		_ctManager.enableChangeTracking(TestPropsValues.getUserId());

		Optional<CTCollection> ctCollectionOptional =
			_ctManager.createCTCollection(
				TestPropsValues.getUserId(), RandomTestUtil.randomString(),
				RandomTestUtil.randomString());

		List<CTCollection> ctCollections = _ctManager.getCTCollections(
			TestPropsValues.getCompanyId());

		Assert.assertEquals(
			"Change collections must have two entries", 2,
			ctCollections.size());
		Assert.assertTrue(ctCollections.contains(ctCollectionOptional.get()));

		Optional<CTCollection> productionCTCollectionOptional =
			_ctManager.getProductionCTCollectionOptional(
				TestPropsValues.getCompanyId());

		Assert.assertTrue(
			ctCollections.contains(productionCTCollectionOptional.get()));
	}

	@Test
	public void testGetCTCollectionsWhenChangeTrackingIsDisabled()
		throws Exception {

		Assert.assertFalse(
			_ctManager.isChangeTrackingEnabled(TestPropsValues.getCompanyId()));

		_ctManager.createCTCollection(
			TestPropsValues.getUserId(), RandomTestUtil.randomString(),
			RandomTestUtil.randomString());

		List<CTCollection> collections = _ctManager.getCTCollections(
			TestPropsValues.getCompanyId());

		Assert.assertTrue(
			"There must not be any change tracking collections",
			ListUtil.isEmpty(collections));
	}

	@Test
	public void testGetCTEntries() throws Exception {
		_ctManager.enableChangeTracking(TestPropsValues.getUserId());

		Optional<CTCollection> ctCollectionOptional =
			_ctManager.createCTCollection(
				TestPropsValues.getUserId(), RandomTestUtil.randomString(),
				RandomTestUtil.randomString());

		Assert.assertTrue(
			"Change tracking collection must be null",
			ctCollectionOptional.isPresent());

		CTCollection ctCollection = ctCollectionOptional.get();

		List<CTEntry> ctEntries = _ctManager.getCTEntries(
			ctCollection.getCtCollectionId());

		Assert.assertTrue(
			"There must not be any change tracking entries",
			ListUtil.isEmpty(ctEntries));

		CTEntry ctEntry = _ctEntryLocalService.addCTEntry(
			TestPropsValues.getUserId(), 0, 0, 0,
			ctCollection.getCtCollectionId(), new ServiceContext());

		ctEntries = _ctManager.getCTEntries(ctCollection.getCtCollectionId());

		Assert.assertEquals(
			"There must be one change tracking entry", 1, ctEntries.size());
		Assert.assertEquals(ctEntry, ctEntries.get(0));
	}

	@Test
	public void testGetProductionCTCollectionOptional() throws Exception {
		_ctManager.enableChangeTracking(TestPropsValues.getUserId());

		Optional<CTCollection> productionCTCollectionOptional =
			_ctManager.getProductionCTCollectionOptional(
				TestPropsValues.getCompanyId());

		Assert.assertTrue(productionCTCollectionOptional.isPresent());

		CTCollection productionCTCollection =
			productionCTCollectionOptional.get();

		Assert.assertEquals(
			CTConstants.CT_COLLECTION_NAME_PRODUCTION,
			productionCTCollection.getName());
	}

	@Test
	public void testGetProductionCTCollectionOptionalWhenChangeTrackingIsDisabled()
		throws Exception {

		Assert.assertFalse(
			_ctManager.isChangeTrackingEnabled(TestPropsValues.getCompanyId()));

		Optional<CTCollection> productionCollectionOptional =
			_ctManager.getProductionCTCollectionOptional(
				TestPropsValues.getCompanyId());

		Assert.assertFalse(productionCollectionOptional.isPresent());
	}

	@Test
	public void testIsChangeTrackingEnabledWhenChangeTrackingIsDisabled()
		throws Exception {

		Assert.assertFalse(
			_ctManager.isChangeTrackingEnabled(TestPropsValues.getCompanyId()));

		_ctManager.enableChangeTracking(TestPropsValues.getUserId());

		Assert.assertTrue(
			_ctManager.isChangeTrackingEnabled(TestPropsValues.getCompanyId()));
	}

	@Test
	public void testIsChangeTrackingEnabledWhenChangeTrackingIsEnabled()
		throws Exception {

		_ctManager.enableChangeTracking(TestPropsValues.getUserId());

		Assert.assertTrue(
			_ctManager.isChangeTrackingEnabled(TestPropsValues.getCompanyId()));

		_ctManager.enableChangeTracking(TestPropsValues.getUserId());

		Assert.assertTrue(
			_ctManager.isChangeTrackingEnabled(TestPropsValues.getCompanyId()));
	}

	@Ignore
	@Test
	public void testIsChangeTrackingSupported() throws Exception {
	}

	@Test
	public void testPublishCTCollection() throws Exception {
		_ctManager.enableChangeTracking(TestPropsValues.getUserId());

		Optional<CTCollection> ctCollectionOptional =
			_ctManager.createCTCollection(
				TestPropsValues.getUserId(), RandomTestUtil.randomString(),
				RandomTestUtil.randomString());

		Assert.assertTrue(ctCollectionOptional.isPresent());

		CTCollection ctCollection = ctCollectionOptional.get();

		CTEntry ctEntry = _ctEntryLocalService.addCTEntry(
			TestPropsValues.getUserId(), 0, 0, 0,
			ctCollection.getCtCollectionId(), new ServiceContext());

		Optional<CTCollection> productionCTCollectionOptional =
			_ctManager.getProductionCTCollectionOptional(
				TestPropsValues.getCompanyId());

		Assert.assertTrue(productionCTCollectionOptional.isPresent());

		CTCollection productionCTCollection =
			productionCTCollectionOptional.get();

		List<CTEntry> productionCTEntries = _ctManager.getCTEntries(
			productionCTCollection.getCtCollectionId());

		Assert.assertTrue(
			"Production change tracking collection must be empty",
			ListUtil.isEmpty(productionCTEntries));

		_ctManager.publishCTCollection(
			TestPropsValues.getUserId(), ctCollection.getCtCollectionId());

		productionCTEntries = _ctManager.getCTEntries(
			productionCTCollection.getCtCollectionId());

		Assert.assertEquals(
			"Production change tracking collection must have one entry", 1,
			productionCTEntries.size());
		Assert.assertEquals(ctEntry, productionCTEntries.get(0));
	}

	@Test
	public void testPublishCTCollectionWhenChangeTrackingIsDisabled()
		throws Exception {

		Assert.assertFalse(
			_ctManager.isChangeTrackingEnabled(TestPropsValues.getCompanyId()));

		CTCollection ctCollection = _ctCollectionLocalService.addCTCollection(
			TestPropsValues.getUserId(), RandomTestUtil.randomString(),
			RandomTestUtil.randomString(), new ServiceContext());

		_ctManager.publishCTCollection(
			TestPropsValues.getUserId(), ctCollection.getCtCollectionId());

		Optional<CTCollection> productionCTCollectionOptional =
			_ctManager.getProductionCTCollectionOptional(
				TestPropsValues.getCompanyId());

		Assert.assertFalse(
			"Production change tracking collection must be null",
			productionCTCollectionOptional.isPresent());
	}

	@Inject
	private CTCollectionLocalService _ctCollectionLocalService;

	@Inject
	private CTEntryLocalService _ctEntryLocalService;

	@Inject
	private CTManager _ctManager;

	@DeleteAfterTestRun
	private User _user;

}