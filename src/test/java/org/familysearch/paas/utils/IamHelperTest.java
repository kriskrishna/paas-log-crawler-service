package org.familysearch.paas.utils;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.ListAccountAliasesResult;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class IamHelperTest {

  private static final String FIRST_ALIAS = "one cool alias";
  @Mock
  private AmazonIdentityManagementClient mockIamClient;
  @Mock
  private ListAccountAliasesResult mockListAccountAliasResult;
  private IamHelper testModel;
  private java.util.List<java.lang.String> aliasList;

  @BeforeMethod
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    aliasList = new ArrayList<>();
    aliasList.add(FIRST_ALIAS);
    aliasList.add("bogus");
    when(mockListAccountAliasResult.getAccountAliases()).thenReturn(aliasList);
    when(mockIamClient.listAccountAliases()).thenReturn(mockListAccountAliasResult);
    testModel = new IamHelper(mockIamClient);
  }

  @Test
  public void itShouldReturnTheFirsAlias() {
    assertEquals(testModel.getFirstAccountAlias(), FIRST_ALIAS);
  }

  @Test
  public void itShouldReturnUnknownOnError() {
    aliasList.clear();
    assertEquals(testModel.getFirstAccountAlias(), "unknown");
  }
}