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

package com.liferay.apio.architect.internal.writer;

import static com.liferay.apio.architect.internal.util.matcher.IsAFunctionalList.aFunctionalListThat;
import static com.liferay.apio.architect.internal.util.representor.MockRepresentorCreator.createRootModelRepresentor;

import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;

import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import static org.junit.Assert.assertEquals;

import com.liferay.apio.architect.internal.alias.PathFunction;
import com.liferay.apio.architect.internal.list.FunctionalList;
import com.liferay.apio.architect.internal.related.RelatedModelImpl;
import com.liferay.apio.architect.internal.request.RequestInfo;
import com.liferay.apio.architect.internal.single.model.SingleModelImpl;
import com.liferay.apio.architect.internal.util.identifier.FirstEmbeddedId;
import com.liferay.apio.architect.internal.util.model.FirstEmbeddedModel;
import com.liferay.apio.architect.internal.util.model.RootModel;
import com.liferay.apio.architect.internal.util.writer.MockWriterUtil;
import com.liferay.apio.architect.related.RelatedModel;
import com.liferay.apio.architect.resource.Resource.Id;
import com.liferay.apio.architect.resource.Resource.Item;
import com.liferay.apio.architect.single.model.SingleModel;
import com.liferay.apio.architect.uri.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Alejandro Hernández
 */
@RunWith(MockitoJUnitRunner.class)
public class FieldsWriterTest {

	@Before
	public void setUp() {
		Mockito.when(
			_requestInfo.getApplicationURL()
		).thenReturn(
			() -> "www.liferay.com/o"
		);
		Mockito.when(
			_requestInfo.getServerURL()
		).thenReturn(
			() -> "www.liferay.com"
		);

		Mockito.when(
			_requestInfo.getFields()
		).thenReturn(
			__ -> string -> true
		);

		Mockito.when(
			_requestInfo.getEmbedded()
		).thenReturn(
			__ -> false
		);

		Mockito.when(
			_requestInfo.getAcceptLanguage()
		).thenReturn(
			Locale::getDefault
		);

		_fieldsWriter = new FieldsWriter<>(
			new SingleModelImpl<>(() -> "first", "root"), _requestInfo,
			createRootModelRepresentor(true), new Path("name", "id"),
			new FunctionalList<>(null, "first"),
			MockWriterUtil::getSingleModel);
	}

	@Test
	public void testGetSingleModel() {
		SingleModel<Integer> parentSingleModel = new SingleModelImpl<>(3, "");

		RelatedModel<Integer, String> relatedModel = new RelatedModelImpl<>(
			"key", FirstEmbeddedId.class, String::valueOf, () -> null);

		Optional<SingleModel<FirstEmbeddedModel>> optional =
			FieldsWriter.getSingleModel(
				relatedModel, parentSingleModel,
				MockWriterUtil::getSingleModel);

		assertThat(optional, is(optionalWithValue()));

		optional.ifPresent(
			singleModel -> {
				assertThat(singleModel.getResourceName(), is("first"));

				FirstEmbeddedModel firstEmbeddedModel = singleModel.getModel();

				assertThat(firstEmbeddedModel.getId(), is("3"));
			});
	}

	@Test
	public void testWithItem() {
		List<Item> items = new ArrayList<>();

		_fieldsWriter.withItem(items::add);

		assertThat(items, hasSize(1));

		Item item = items.get(0);

		assertThat(item.getName(), is("root"));

		Optional<Id> optional = item.getIdOptional();

		assertThat(optional, is(optionalWithValue()));

		optional.ifPresent(id -> assertEquals(id, Id.of("first", "id")));
	}

	@Test
	public void testWriteApplicationRelativeURLFields() {
		Map<String, String> strings = new HashMap<>();

		_fieldsWriter.writeApplicationRelativeURLFields(strings::put);

		assertThat(strings, is(aMapWithSize(1)));
		assertThat(
			strings,
			hasEntry("applicationRelativeURL1", "www.liferay.com/o/first"));
	}

	@Test
	public void testWriteApplicationRelativeURLFieldsWithFilter() {
		Mockito.when(
			_requestInfo.getFields()
		).thenReturn(
			list -> "applicationRelativeURL1"::equals
		);

		Map<String, String> strings = new HashMap<>();

		_fieldsWriter.writeApplicationRelativeURLFields(strings::put);

		assertThat(strings, is(aMapWithSize(1)));
		assertThat(
			strings,
			hasEntry("applicationRelativeURL1", "www.liferay.com/o/first"));
	}

	@Test
	public void testWriteBinaries() {
		Map<String, String> binaries = new HashMap<>();

		_fieldsWriter.writeBinaries(binaries::put);

		assertThat(binaries, is(aMapWithSize(2)));
		assertThat(
			binaries, hasEntry("binary1", "www.liferay.com/o/name/id/binary1"));
		assertThat(
			binaries, hasEntry("binary2", "www.liferay.com/o/name/id/binary2"));
	}

	@Test
	public void testWriteBinariesWithFieldsFilter() {
		Mockito.when(
			_requestInfo.getFields()
		).thenReturn(
			list -> "binary2"::equals
		);

		Map<String, String> binaries = new HashMap<>();

		_fieldsWriter.writeBinaries(binaries::put);

		assertThat(binaries, is(aMapWithSize(1)));
		assertThat(
			binaries, hasEntry("binary2", "www.liferay.com/o/name/id/binary2"));
	}

	@Test
	public void testWriteBooleanFields() {
		Map<String, Boolean> booleans = new HashMap<>();

		_fieldsWriter.writeBooleanFields(booleans::put);

		assertThat(booleans, is(aMapWithSize(2)));
		assertThat(booleans, hasEntry("boolean1", true));
		assertThat(booleans, hasEntry("boolean2", false));
	}

	@Test
	public void testWriteBooleanFieldsWithFieldsFilter() {
		Mockito.when(
			_requestInfo.getFields()
		).thenReturn(
			list -> "boolean2"::equals
		);

		Map<String, Boolean> booleans = new HashMap<>();

		_fieldsWriter.writeBooleanFields(booleans::put);

		assertThat(booleans, is(aMapWithSize(1)));
		assertThat(booleans, hasEntry("boolean2", false));
	}

	@Test
	public void testWriteBooleanListFields() {
		Map<String, List<Boolean>> booleans = new HashMap<>();

		_fieldsWriter.writeBooleanListFields(booleans::put);

		assertThat(booleans, is(aMapWithSize(2)));
		assertThat(
			booleans,
			hasEntry("booleanList1", asList(true, true, false, false)));
		assertThat(
			booleans,
			hasEntry("booleanList2", asList(true, false, true, false)));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testWriteEmbeddedRelatedModelsWithEmbeddedPredicate() {
		List<String> linkedRelatedModelURLs = new ArrayList<>();
		List<String> embeddedRelatedModelURLs = new ArrayList<>();
		List<FunctionalList<String>> firstEmbeddedPathElementsList =
			new ArrayList<>();
		List<FunctionalList<String>> secondEmbeddedPathElementsList =
			new ArrayList<>();
		List<FunctionalList<String>> thirdEmbeddedPathElementsList =
			new ArrayList<>();
		List<SingleModel> singleModels = new ArrayList<>();

		PathFunction pathFunction = Mockito.mock(PathFunction.class);

		Mockito.when(
			_requestInfo.getEmbedded()
		).thenReturn(
			"first.embedded2"::equals
		);

		Mockito.when(
			pathFunction.apply(Mockito.any(), Mockito.any())
		).thenReturn(
			Optional.of(new Path("name1", "id1")),
			Optional.of(new Path("name2", "id2")),
			Optional.of(new Path("name3", "id3")),
			Optional.of(new Path("name4", "id4"))
		);

		_fieldsWriter.writeRelatedModels(
			pathFunction,
			(singleModel, embeddedPathElements) -> {
				singleModels.add(singleModel);
				firstEmbeddedPathElementsList.add(embeddedPathElements);
			},
			(url, embeddedPathElements) -> {
				linkedRelatedModelURLs.add(url);
				secondEmbeddedPathElementsList.add(embeddedPathElements);
			},
			(url, embeddedPathElements) -> {
				embeddedRelatedModelURLs.add(url);
				thirdEmbeddedPathElementsList.add(embeddedPathElements);
			});

		assertThat(singleModels, hasSize(equalTo(1)));

		SingleModel singleModel = singleModels.get(0);

		assertThat(
			singleModel.getModel(), is(instanceOf(FirstEmbeddedModel.class)));

		assertThat(linkedRelatedModelURLs, hasSize(equalTo(3)));
		assertThat(
			linkedRelatedModelURLs,
			contains(
				"www.liferay.com/o/name1/id1", "www.liferay.com/o/name3/id3",
				"www.liferay.com/o/name4/id4"));

		assertThat(embeddedRelatedModelURLs, hasSize(equalTo(1)));
		assertThat(
			embeddedRelatedModelURLs, contains("www.liferay.com/o/name2/id2"));

		assertThat(firstEmbeddedPathElementsList, hasSize(equalTo(1)));
		assertThat(
			firstEmbeddedPathElementsList,
			contains(aFunctionalListThat(contains("first", "embedded2"))));

		assertThat(secondEmbeddedPathElementsList, hasSize(equalTo(3)));
		assertThat(
			secondEmbeddedPathElementsList,
			contains(
				aFunctionalListThat(contains("first", "embedded1")),
				aFunctionalListThat(contains("first", "linked1")),
				aFunctionalListThat(contains("first", "linked2"))));

		assertThat(thirdEmbeddedPathElementsList, hasSize(equalTo(1)));
		assertThat(
			thirdEmbeddedPathElementsList,
			contains(aFunctionalListThat(contains("first", "embedded2"))));
	}

	@Test
	public void testWriteEmbeddedRelatedModelsWithFieldsFilter() {
		Mockito.when(
			_requestInfo.getFields()
		).thenReturn(
			list -> "embedded2"::equals
		);

		List<String> linkedRelatedModelURLs = new ArrayList<>();
		List<FunctionalList<String>> linkedPathElementsList = new ArrayList<>();

		PathFunction pathFunction = Mockito.mock(PathFunction.class);

		Mockito.when(
			pathFunction.apply(Mockito.any(), Mockito.any())
		).thenReturn(
			Optional.of(new Path("name2", "id2"))
		);

		_fieldsWriter.writeRelatedModels(
			pathFunction,
			(singleModel, embeddedPathElements) ->
				Assert.fail("Should not be embedded"),
			(url, embeddedPathElements) -> {
				linkedRelatedModelURLs.add(url);
				linkedPathElementsList.add(embeddedPathElements);
			},
			(url, embeddedPathElements) -> Assert.fail(
				"Should not be embedded"));

		assertThat(linkedRelatedModelURLs, hasSize(equalTo(1)));
		assertThat(
			linkedRelatedModelURLs, contains("www.liferay.com/o/name2/id2"));

		assertThat(
			linkedPathElementsList,
			contains(aFunctionalListThat(contains("first", "embedded2"))));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testWriteEmbeddedRelatedModelsWithoutEmbeddedPredicate() {
		List<String> linkedRelatedModelURLs = new ArrayList<>();
		List<FunctionalList<String>> embeddedPathElementsList =
			new ArrayList<>();

		PathFunction pathFunction = Mockito.mock(PathFunction.class);

		Mockito.when(
			pathFunction.apply(Mockito.any(), Mockito.any())
		).thenReturn(
			Optional.of(new Path("name1", "id1")),
			Optional.of(new Path("name2", "id2")),
			Optional.of(new Path("name3", "id3")),
			Optional.of(new Path("name4", "id4"))
		);

		_fieldsWriter.writeRelatedModels(
			pathFunction,
			(singleModel, embeddedPathElements) ->
				Assert.fail("Should not be embedded"),
			(url, embeddedPathElements) -> {
				linkedRelatedModelURLs.add(url);
				embeddedPathElementsList.add(embeddedPathElements);
			},
			(url, embeddedPathElements) -> Assert.fail(
				"Should not be embedded"));

		assertThat(linkedRelatedModelURLs, hasSize(equalTo(4)));
		assertThat(
			linkedRelatedModelURLs,
			contains(
				"www.liferay.com/o/name1/id1", "www.liferay.com/o/name2/id2",
				"www.liferay.com/o/name3/id3", "www.liferay.com/o/name4/id4"));

		assertThat(
			embeddedPathElementsList,
			contains(
				aFunctionalListThat(contains("first", "embedded1")),
				aFunctionalListThat(contains("first", "embedded2")),
				aFunctionalListThat(contains("first", "linked1")),
				aFunctionalListThat(contains("first", "linked2"))));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testWriteLinkedRelatedModels() {
		List<String> linkedRelatedModelsURLs = new ArrayList<>();
		List<FunctionalList<String>> embeddedPathElementsList =
			new ArrayList<>();

		PathFunction pathFunction = Mockito.mock(PathFunction.class);

		Mockito.when(
			pathFunction.apply(Mockito.any(), Mockito.any())
		).thenReturn(
			Optional.of(new Path("name1", "id1")),
			Optional.of(new Path("name2", "id2")),
			Optional.of(new Path("name3", "id3")),
			Optional.of(new Path("name4", "id4"))
		);

		_fieldsWriter.writeRelatedModels(
			pathFunction,
			(singleModel, embeddedPathElements) -> {
			},
			(url, embeddedPathElements) -> {
				linkedRelatedModelsURLs.add(url);
				embeddedPathElementsList.add(embeddedPathElements);
			},
			(url, embeddedPathElements) -> {
			});

		assertThat(linkedRelatedModelsURLs, hasSize(equalTo(4)));
		assertThat(
			linkedRelatedModelsURLs,
			contains(
				"www.liferay.com/o/name1/id1", "www.liferay.com/o/name2/id2",
				"www.liferay.com/o/name3/id3", "www.liferay.com/o/name4/id4"));

		assertThat(
			embeddedPathElementsList,
			contains(
				aFunctionalListThat(contains("first", "embedded1")),
				aFunctionalListThat(contains("first", "embedded2")),
				aFunctionalListThat(contains("first", "linked1")),
				aFunctionalListThat(contains("first", "linked2"))));
	}

	@Test
	public void testWriteLinkedRelatedModelsWithFieldsFilter() {
		Mockito.when(
			_requestInfo.getFields()
		).thenReturn(
			list -> "linked2"::equals
		);

		List<String> linkedRelatedModelsURLs = new ArrayList<>();
		List<FunctionalList<String>> embeddedPathElementsList =
			new ArrayList<>();

		PathFunction pathFunction = Mockito.mock(PathFunction.class);

		Mockito.when(
			pathFunction.apply(Mockito.any(), Mockito.any())
		).thenReturn(
			Optional.of(new Path("name2", "id2"))
		);

		_fieldsWriter.writeRelatedModels(
			pathFunction,
			(singleModel, embeddedPathElements) -> {
			},
			(url, embeddedPathElements) -> {
				linkedRelatedModelsURLs.add(url);
				embeddedPathElementsList.add(embeddedPathElements);
			},
			(url, embeddedPathElements) -> {
			});

		assertThat(linkedRelatedModelsURLs, hasSize(equalTo(1)));
		assertThat(
			linkedRelatedModelsURLs, contains("www.liferay.com/o/name2/id2"));

		assertThat(
			embeddedPathElementsList,
			contains(aFunctionalListThat(contains("first", "linked2"))));
	}

	@Test
	public void testWriteLinks() {
		Map<String, String> links = new HashMap<>();

		_fieldsWriter.writeLinks(links::put);

		assertThat(links, is(aMapWithSize(2)));
		assertThat(links, hasEntry("link1", "www.liferay.com"));
		assertThat(links, hasEntry("link2", "community.liferay.com"));
	}

	@Test
	public void testWriteLinksWithFilter() {
		Mockito.when(
			_requestInfo.getFields()
		).thenReturn(
			list -> "link2"::equals
		);

		Map<String, String> links = new HashMap<>();

		_fieldsWriter.writeLinks(links::put);

		assertThat(links, is(aMapWithSize(1)));
		assertThat(links, hasEntry("link2", "community.liferay.com"));
	}

	@Test
	public void testWriteLocalizedStringFields() {
		Map<String, String> localizedStrings = new HashMap<>();

		_fieldsWriter.writeLocalizedStringFields(localizedStrings::put);

		assertThat(localizedStrings, is(aMapWithSize(2)));
		assertThat(
			localizedStrings, hasEntry("localizedString1", "Translated 1"));
		assertThat(
			localizedStrings, hasEntry("localizedString2", "Translated 2"));
	}

	@Test
	public void testWriteLocalizedStringFieldsWithFilter() {
		Mockito.when(
			_requestInfo.getFields()
		).thenReturn(
			list -> "localizedString2"::equals
		);

		Map<String, String> localizedStrings = new HashMap<>();

		_fieldsWriter.writeLocalizedStringFields(localizedStrings::put);

		assertThat(localizedStrings, is(aMapWithSize(1)));
		assertThat(
			localizedStrings, hasEntry("localizedString2", "Translated 2"));
	}

	@Test
	public void testWriteNumberFields() {
		Map<String, Number> numbers = new HashMap<>();

		_fieldsWriter.writeNumberFields(numbers::put);

		assertThat(numbers, is(aMapWithSize(2)));
		assertThat(numbers, hasEntry("number1", 2017));
		assertThat(numbers, hasEntry("number2", 42));
	}

	@Test
	public void testWriteNumberFieldsWithFilter() {
		Mockito.when(
			_requestInfo.getFields()
		).thenReturn(
			list -> "number2"::equals
		);

		Map<String, Number> numbers = new HashMap<>();

		_fieldsWriter.writeNumberFields(numbers::put);

		assertThat(numbers, is(aMapWithSize(1)));
		assertThat(numbers, hasEntry("number2", 42));
	}

	@Test
	public void testWriteNumberListFields() {
		Map<String, List<Number>> numbers = new HashMap<>();

		_fieldsWriter.writeNumberListFields(numbers::put);

		assertThat(numbers, is(aMapWithSize(2)));
		assertThat(numbers, hasEntry("numberList1", asList(1, 2, 3, 4, 5)));
		assertThat(numbers, hasEntry("numberList2", asList(6, 7, 8, 9, 10)));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testWriteRelatedCollections() {
		List<String> relatedCollectionURLs = new ArrayList<>();
		List<FunctionalList<String>> embeddedPathElementsList =
			new ArrayList<>();

		Function<String, Optional<String>> nameFunction = Mockito.mock(
			Function.class);

		Mockito.when(
			nameFunction.apply(Mockito.any())
		).thenReturn(
			Optional.of("first"), Optional.of("second")
		);

		_fieldsWriter.writeRelatedCollections(
			null, nameFunction,
			(url, embeddedPathElements) -> {
				relatedCollectionURLs.add(url);
				embeddedPathElementsList.add(embeddedPathElements);
			});

		assertThat(relatedCollectionURLs, hasSize(equalTo(2)));
		assertThat(
			relatedCollectionURLs,
			contains(
				"www.liferay.com/o/root/id/first",
				"www.liferay.com/o/root/id/second"));

		assertThat(
			embeddedPathElementsList,
			contains(
				aFunctionalListThat(contains("first", "relatedCollection1")),
				aFunctionalListThat(contains("first", "relatedCollection2"))));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testWriteRelatedCollectionsWithFilter() {
		Mockito.when(
			_requestInfo.getFields()
		).thenReturn(
			list -> "relatedCollection2"::equals
		);

		List<String> relatedCollectionURLs = new ArrayList<>();
		List<FunctionalList<String>> embeddedPathElementsList =
			new ArrayList<>();

		Function<String, Optional<String>> nameFunction = Mockito.mock(
			Function.class);

		Mockito.when(
			nameFunction.apply(Mockito.any())
		).thenReturn(
			Optional.of("first"), Optional.of("second")
		);

		_fieldsWriter.writeRelatedCollections(
			null, nameFunction,
			(url, embeddedPathElements) -> {
				relatedCollectionURLs.add(url);
				embeddedPathElementsList.add(embeddedPathElements);
			});

		assertThat(relatedCollectionURLs, hasSize(equalTo(1)));
		assertThat(
			relatedCollectionURLs,
			contains("www.liferay.com/o/root/id/second"));

		assertThat(
			embeddedPathElementsList,
			contains(
				aFunctionalListThat(contains("first", "relatedCollection2"))));
	}

	@Test
	public void testWriteRelativeURLFields() {
		Mockito.when(
			_requestInfo.getServerURL()
		).thenReturn(
			() -> "localhost"
		);

		Map<String, String> strings = new HashMap<>();

		_fieldsWriter.writeRelativeURLFields(strings::put);

		assertThat(strings, is(aMapWithSize(2)));
		assertThat(strings, hasEntry("relativeURL1", "localhost/first"));
		assertThat(strings, hasEntry("relativeURL2", "localhost/second"));
	}

	@Test
	public void testWriteRelativeURLFieldsWithFilter() {
		Mockito.when(
			_requestInfo.getServerURL()
		).thenReturn(
			() -> "localhost"
		);

		Mockito.when(
			_requestInfo.getFields()
		).thenReturn(
			list -> "relativeURL2"::equals
		);

		Map<String, String> strings = new HashMap<>();

		_fieldsWriter.writeRelativeURLFields(strings::put);

		assertThat(strings, is(aMapWithSize(1)));
		assertThat(strings, hasEntry("relativeURL2", "localhost/second"));
	}

	@Test
	public void testWriteSingleURL() {
		_fieldsWriter.writeSingleURL(
			url -> assertThat(url, is("www.liferay.com/o/name/id")));
	}

	@Test
	public void testWriteStringFields() {
		Map<String, String> strings = new HashMap<>();

		_fieldsWriter.writeStringFields(strings::put);

		assertThat(strings, is(aMapWithSize(4)));
		assertThat(strings, hasEntry("string1", "Live long and prosper"));
		assertThat(strings, hasEntry("string2", "Hypermedia"));
		assertThat(strings, hasEntry("date1", "2016-06-15T09:00Z"));
		assertThat(strings, hasEntry("date2", "2017-04-03T18:36Z"));
	}

	@Test
	public void testWriteStringFieldsWithFilter() {
		Mockito.when(
			_requestInfo.getFields()
		).thenReturn(
			list -> "string2"::equals
		);

		Map<String, String> strings = new HashMap<>();

		_fieldsWriter.writeStringFields(strings::put);

		assertThat(strings, is(aMapWithSize(1)));
		assertThat(strings, hasEntry("string2", "Hypermedia"));
	}

	@Test
	public void testWriteStringListFields() {
		Map<String, List<String>> strings = new HashMap<>();

		_fieldsWriter.writeStringListFields(strings::put);

		assertThat(strings, is(aMapWithSize(2)));
		assertThat(
			strings, hasEntry("stringList1", asList("a", "b", "c", "d", "e")));
		assertThat(
			strings, hasEntry("stringList2", asList("f", "g", "h", "i", "j")));
	}

	@Test
	public void testWriteTypes() {
		List<String> types = new ArrayList<>();

		_fieldsWriter.writeTypes(types::addAll);

		assertThat(types, contains("Type 1", "Type 2"));
	}

	private FieldsWriter<RootModel> _fieldsWriter;
	private final RequestInfo _requestInfo = Mockito.mock(RequestInfo.class);

}