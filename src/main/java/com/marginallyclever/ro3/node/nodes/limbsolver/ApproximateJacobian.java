package com.marginallyclever.ro3.node.nodes.limbsolver;

import com.marginallyclever.convenience.helpers.BigMatrixHelper;
import com.marginallyclever.convenience.helpers.StringHelper;

/**
 * {@link ApproximateJacobian} is used to calculate the
 * <a href="https://en.wikipedia.org/wiki/Jacobian_matrix_and_determinant">Jacobian matrix</a> for a robot arm.
 * Each implementation can derive this class and fill in the jacobian matrix.
 * <p>This class provides several functionalities:</p>
 * <ul>
 * <li>It can get the inverse Jacobian matrix.</li>
 * <li>It can get the joint velocity from the Cartesian velocity.</li>
 * <li>It can get the Cartesian velocity from the joint velocity.</li>
 * <li>It can get the Jacobian matrix.</li>
 * <li>It can get the time derivative of the Jacobian matrix.</li>
 * <li>It can get the Coriolis term.</li>
 * </ul>
 */
public abstract class ApproximateJacobian {
    /**
     * a matrix that will be filled with the jacobian. The first three columns
     * are translation component. The last three columns are the rotation component.
     */
    protected final double[][] jacobian;
    protected final int DOF;

    protected ApproximateJacobian(int DOF) {
        this.DOF = DOF;
        jacobian = new double[6][DOF];
    }

    /**
     * See <a href="https://stackoverflow.com/a/53028167/1159440">5 DOF Inverse kinematics for Jacobian Matrices</a>.
     * @return the inverse Jacobian matrix.
     */
    private double[][] getInverseJacobian() {
        int rows = jacobian.length;
        int cols = jacobian[0].length;
        if(rows<cols) return getPseudoInverseOverdetermined();
        else if (rows>cols) return getPseudoInverseUnderdetermined();
        else {
            return getInverseDampedLeastSquares(0.0001);
            //return MatrixHelper.invert(jacobian);  // old way
        }
    }

    /**
     * Moore-Penrose pseudo-inverse for over-determined systems.
     * J_plus = J.transpose * (J*J.transpose()).inverse() // This is for
     * @return the pseudo-inverse of the jacobian matrix.
     */
    private double[][] getPseudoInverseOverdetermined() {
        double[][] jt = BigMatrixHelper.transpose(jacobian);
        double[][] mm = BigMatrixHelper.multiplyMatrices(jacobian, jt);
        double[][] ji = BigMatrixHelper.invert(mm);
        return BigMatrixHelper.multiplyMatrices(jt, ji);
    }

    /**
     * Moore-Penrose pseudo-inverse for under-determined systems.
     * J_plus = (J.transpose()*J).inverse() * J.transpose()
     * @return the pseudo-inverse of the jacobian matrix.
     */
    private double[][] getPseudoInverseUnderdetermined() {
        double[][] jt = BigMatrixHelper.transpose(jacobian);
        double[][] mm = BigMatrixHelper.multiplyMatrices(jt, jacobian);
        double[][] ji = BigMatrixHelper.invert(mm);
        return BigMatrixHelper.multiplyMatrices(ji, jt);
    }

    private double[][] getInverseDampedLeastSquares(double lambda) {
        double[][] jt = BigMatrixHelper.transpose(jacobian);
        double[][] jjt = BigMatrixHelper.multiplyMatrices(jacobian, jt);

        // Add lambda^2 * identity matrix to jjt
        for (int i = 0; i < jacobian.length; i++) {
            jjt[i][i] += lambda * lambda;
        }

        double[][] jjt_inv = BigMatrixHelper.invert(jjt);
        return BigMatrixHelper.multiplyMatrices(jt, jjt_inv);
    }

    /**
     * Use the Jacobian to get the joint velocity from the cartesian velocity.
     * @param cartesianVelocity 6 doubles - the XYZ translation and UVW rotation forces on the end effector.
     *                          The rotation component is in radians.
     * @return joint velocity in degrees.  Will be filled with the new velocity.
     * @throws Exception if joint velocities have NaN values
     */
    public double[] getJointFromCartesian(final double[] cartesianVelocity) throws Exception {
        double[][] inverseJacobian = getInverseJacobian();
        double[] jointVelocity = new double[DOF];

        // vector-matrix multiplication (y = x^T A)
        for (int j=0; j<jointVelocity.length; ++j) {
            double sum = 0;
            for (int k=0; k<cartesianVelocity.length; ++k) {
                sum += inverseJacobian[j][k] * cartesianVelocity[k];
            }
            if (Double.isNaN(sum)) {
                throw new Exception("Bad inverse Jacobian.  Singularity?");
            }
            jointVelocity[j] = Math.toDegrees(sum);
        }

        return jointVelocity;
    }

    /**
     * Use the jacobian to convert joint velocity to cartesian velocity.
     * @param joint joint velocity in degrees.
     * @return 6 doubles containing the XYZ translation and UVW rotation forces on the end effector.
     * The rotation component is in radians.
     */
    public double[] getCartesianFromJoint(final double[] joint) {
        // vector-matrix multiplication (y = x^T A)
        double[] cartesianVelocity = new double[DOF];
        for (int j = 0; j < DOF; ++j) {
            double sum = 0;
            for (int k = 0; k < 6; ++k) {
                sum += jacobian[k][j] * Math.toRadians(joint[j]);
            }
            cartesianVelocity[j] = sum;
        }
        return cartesianVelocity;
    }

    public double[][] getJacobian() {
        return jacobian;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Jacobian:\n");
        for (double[] doubles : jacobian) {
            sb.append("[");
            for (int j = 0; j < doubles.length; j++) {
                sb.append(StringHelper.formatDouble(doubles[j]));
                if (j < doubles.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]\n");
        }
        return sb.toString();
    }

    /**
     * The time derivative is calculated by multiplying the jacobian by the joint velocities.
     * You can do this by taking the derivative of each element of the Jacobian matrix individually.
     * @param jointVelocities joint velocities in radians.  In classical notation this would be qDot.
     * @return the time derivative of the Jacobian matrix.
     */
    public double[][] getTimeDerivative(double [] jointVelocities) {
        if(jointVelocities.length!=DOF) throw new IllegalArgumentException("jointVelocities must be the same length as the number of joints.");

        double[][] jacobianDot = new double[6][DOF];  // Initialize the derivative of the Jacobian matrix

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < DOF; j++) {
                // Calculate the derivative of each element of the Jacobian
                double derivative = 0.0;
                for (int k = 0; k < DOF; k++) {
                    derivative += jacobian[i][k] * jointVelocities[k];  // Assuming qDot represents joint velocities
                }
                jacobianDot[i][j] = derivative;
            }
        }
        return jacobianDot;
    }

    /**
     * Calculate the coriolis term.
     * @param jointVelocities joint velocities in radians.  In classical notation this would be qDot.
     * @return the coriolis term.
     */
    public double[] getCoriolisTerm(double[] jointVelocities) {
        if(jointVelocities.length!=DOF) throw new IllegalArgumentException("jointVelocities must be the same length as the number of joints.");

        // Initialize the coriolis term vector with zeros
        double[] coriolisTerm = new double[DOF];

        for (int i = 0; i < DOF; i++) {
            for (int j = 0; j < DOF; j++) {
                for (int k = 0; k < DOF; k++) {
                    // Calculate the Coriolis term contribution for joint i
                    double v = jacobian[k][i] * jacobian[k][j];
                    coriolisTerm[i] += -0.5 * (
                            v * jointVelocities[i] +
                            v * jointVelocities[j] -
                            v * jointVelocities[k]);
                }
            }
        }

        return coriolisTerm;
    }
}
