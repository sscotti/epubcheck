package org.w3c.epubcheck.test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import com.adobe.epubcheck.api.EPUBProfile;
import com.adobe.epubcheck.api.EpubCheck;
import com.adobe.epubcheck.api.Report;
import com.adobe.epubcheck.messages.Severity;
import com.adobe.epubcheck.nav.NavChecker;
import com.adobe.epubcheck.opf.DocumentValidator;
import com.adobe.epubcheck.opf.OPFCheckerFactory;
import com.adobe.epubcheck.opf.ValidationContext.ValidationContextBuilder;
import com.adobe.epubcheck.ops.OPSChecker;
import com.adobe.epubcheck.overlay.OverlayChecker;
import com.adobe.epubcheck.util.Archive;
import com.adobe.epubcheck.util.EPUBVersion;
import com.adobe.epubcheck.util.FileResourceProvider;
import com.adobe.epubcheck.util.ReportingLevel;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

public class ExecutionSteps
{

  public enum CheckerMode
  {
    EPUB,
    MEDIA_OVERLAYS_DOC,
    NAVIGATION_DOC,
    PACKAGE_DOC,
    SVG_CONTENT_DOC,
    XHTML_CONTENT_DOC
  }

  private TestReport report;
  private String basepath = "";
  // TODO replace by a checker builder
  private CheckerMode mode = CheckerMode.EPUB;
  private EPUBVersion version = EPUBVersion.VERSION_3;
  private EPUBProfile profile = EPUBProfile.DEFAULT;

  public ExecutionSteps(TestReport report)
  {
    this.report = report;
  }

  @Before("@debug")
  public void beforeDebug()
  {
    report.setVerbose(true);
  }

  @After("@debug")
  public void afterDebug()
  {
    report.setVerbose(false);
  }

  @Given("(EPUB )test files located at {string}")
  public void setBasePath(String path)
  {
    this.basepath = path;
  }

  @And("EPUBCheck with default settings")
  public void configureDefaults()
  {
    // nothing to do
  }

  @And("EPUBCheck configured to check a(n) {checkerMode}")
  public void configureCheckerMode(ExecutionSteps.CheckerMode mode)
  {
    this.mode = mode;
  }
  
  @And("(the) reporting level (is )set to {severity}")
  public void configureReportingLevel(Severity severity) {
    report.setReportingLevel(ReportingLevel.getReportingLevel(severity));
  }

  @When("checking EPUB/file/document {string}")
  public void check(String path)
  {
    File testFile = getEPUBFile(basepath + path);
    DocumentValidator checker = getChecker(mode, testFile, version, profile, report);
    checker.validate();
  }

  private DocumentValidator getChecker(CheckerMode mode, File file, EPUBVersion version,
      EPUBProfile profile, Report report)
  {
    switch (mode)
    {
    case MEDIA_OVERLAYS_DOC:
      return new OverlayChecker(
          new ValidationContextBuilder().mimetype("application/smil+xml").path(file.getPath())
              .resourceProvider(new FileResourceProvider(file.getPath())).report(report).build());
    case NAVIGATION_DOC:
      return new NavChecker(new ValidationContextBuilder().path(file.getPath())
          .resourceProvider(new FileResourceProvider(file.getPath())).report(report)
          .mimetype("application/xhtml+xml").version(EPUBVersion.VERSION_3)
          .profile(EPUBProfile.DEFAULT).build());
    case PACKAGE_DOC:
      return OPFCheckerFactory.getInstance()
          .newInstance(new ValidationContextBuilder().path(file.getPath())
              .resourceProvider(new FileResourceProvider(file.getPath())).report(report)
              .version(version).profile(profile).build());
    case SVG_CONTENT_DOC:
      return new OPSChecker(
          new ValidationContextBuilder().path(file.getPath()).mimetype("application/xhtml+xml")
              .resourceProvider(new FileResourceProvider(file.getPath())).report(report)
              .version(version).profile(profile).build());
    case XHTML_CONTENT_DOC:
      return new OPSChecker(
          new ValidationContextBuilder().path(file.getPath()).mimetype("application/xhtml+xml")
              .resourceProvider(new FileResourceProvider(file.getPath())).report(report)
              .version(version).profile(profile).build());
    case EPUB:
    default:
      return new EpubCheck(file, report, profile);
    }
  }

  private File getEPUBFile(String path)
  {
    try
    {
      URL url = this.getClass().getResource(path);
      assertThat("Couldn’t find EPUB: " + path, url, is(notNullValue()));
      File file = new File(url.toURI());
      if (file.isDirectory())
      {
        Archive epub = new Archive(file.getPath());
        epub.createArchive();
        return epub.getEpubFile();
      }
      else
      {
        return file;
      }
    } catch (URISyntaxException e)
    {
      throw new IllegalStateException(e);
    }
  }
}