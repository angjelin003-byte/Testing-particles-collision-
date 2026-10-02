package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.physics.FourVector
import com.example.physics.RelativisticCollisionEngine
import com.example.physics.StandardModelCatalog
import com.example.physics.Vector3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Collider 3D", appName)
  }

  @Test
  fun `test relativistic four-vector invariant mass`() {
    val photon = StandardModelCatalog.PHOTON
    val fv = FourVector.fromParticle(photon, Vector3D(3f, 4f, 0f))
    assertEquals(5.0, fv.e, 0.001)
    assertEquals(0.0, fv.invariantMass(), 0.001)
  }

  @Test
  fun `test physical multiplicity scaling`() {
    val (nch, nTotal) = RelativisticCollisionEngine.calculatePhysicalMultiplicity(
      StandardModelCatalog.PROTON,
      StandardModelCatalog.PROTON,
      13600.0,
      0.2,
      Random(42)
    )
    assertTrue("Charged multiplicity should be positive", nch > 5)
    assertTrue("Total multiplicity should exceed charged multiplicity", nTotal >= nch)
  }
}
