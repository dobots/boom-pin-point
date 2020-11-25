#!/usr/bin/env python

############################
#This script computes the location of a point using the
#trilateration algorithm obtained from 
#"https://arrow.tudublin.ie/cgi/viewcontent.cgi?article=1000&context=dmcthes"
############################




#Definitions
import rospy
from std_msgs.msg import String
import numpy as np
import matplotlib.pyplot as plt
import math



#Function to sort nested list by values in second collumn
def sort_by_sublist(sub_li): 
    l = len(sub_li) 
    for i in range(0, l): 
        for j in range(0, l-i-1): 
            if (sub_li[j][1] > sub_li[j + 1][1]): 
                tempo = sub_li[j] 
                sub_li[j]= sub_li[j + 1] 
                sub_li[j + 1]= tempo 
    return sub_li 
    



#measurements consist of the coordinates of the measurement and the measured timestamp/distances relative
#to an unknown point. 
#	Example:
#		measurements = [[(0,0), 4.5], [(0,20), 1.5], [(30,0), 8.5]]
def localize_sound_origin(measurements, amountof_unknowns):

	#Sort the measurements smallest to largest measurement
	measurements = sort_by_sublist(measurements)
	
	m_coordinates_x = [] 	#The X-coordinates of all measurements
	m_coordinates_y = [] 	#The Y-coordinates of all measurements
	Xp, Yp = 0, 0	      	#The correction values
	
	distances = []		
	distances_n = []
	
	#Save all cooredinates and substract smalles measurement from all other measurements
	for index, measurement in enumerate(measurements):
		m_coordinates_x.append(measurement[0][0])
		m_coordinates_y.append(measurement[0][1])
		
		distances.append(measurement[1] - measurements[0][1])
		distances_n.append(measurement[1] - measurements[0][1])
		
		Xp += measurement[0][0]
		Yp += measurement[0][1]
	
	#Estimate a position using the sum of all measurement coordinates	
	Xp = Xp / len(measurements)
	Yp = Yp / len(measurements)		

	X_matrix = []
	deviation = []
	
	
	#Set first measurement to 0
	distances[0] = 0.0
	
	iterations = 0
	
	#Recalculate the computed values until the corrections are small enough or if
	#an iteration limit is reached.
	for iteration in range(0, 1000):
	
		A_mat = np.zeros((len(measurements)-1, 2))
		L_mat = np.zeros((len(measurements)-1, 1))
		
		distances_n[0] = np.sqrt(
					np.power(Xp - m_coordinates_x[0], 2) + 
					np.power(Yp - m_coordinates_y[0], 2))
		
		for i in range(1, len(measurements)):
			distances_n[i] = (
					np.sqrt(np.power(Xp - m_coordinates_x[i], 2) + 
					np.power(Yp - m_coordinates_y[i], 2))) - distances_n[0]
						   
				
			A_mat[i - 1][0] = (
					(Xp - m_coordinates_x[i]) / (distances_n[0] + distances[i]) -
					(Xp - m_coordinates_x[0]) / distances_n[0]
					) 
						
			A_mat[i - 1][1] = (
					(Yp - m_coordinates_y[i]) / (distances_n[0] + distances[i]) -
					(Yp - m_coordinates_y[0]) / distances_n[0]
					) 
						
			L_mat[i - 1][0] = np.double(distances[i] - distances_n[i])
			
		A_matrix = A_mat.copy()
		L_matrix = L_mat.copy()
		
		A_matrix_transposed = np.transpose(A_matrix)
		A_matrix_transposed_A = np.dot(A_matrix_transposed, A_matrix)
		A_matrix_transposed_L = np.dot(A_matrix_transposed, L_matrix)
		A_matrix_transposed_A_inv = np.linalg.inv(A_matrix_transposed_A)
		
		X_matrix = np.dot(A_matrix_transposed_A_inv, A_matrix_transposed_L)
		
		V_matrix = np.matmul(A_matrix , X_matrix)
		V_matrix = np.subtract(V_matrix , L_matrix)		
		V_matrix_transposed = np.transpose(V_matrix)
		V_matrix_transposed = np.matmul(V_matrix_transposed, V_matrix)
		
		deviation_unit_weight = np.double(np.sqrt(V_matrix_transposed[0] / (A_matrix.shape[0] - A_matrix.shape[1])))
			
		#Calculate current deviation
		deviation = []
		for j in range(0, amountof_unknowns):
			deviation.append(deviation_unit_weight * np.sqrt(A_matrix_transposed_A_inv[j][j]))
			
		Xp = Xp + X_matrix[0]
		Yp = Yp + X_matrix[1]
		
		
		#End while loop if the correction is small enough
		if not np.absolute(X_matrix[0] + X_matrix[1]) > 0.01:
			break 
		
		print("Sound location estimate: {}, {}".format(Xp, Yp))
		return (Xp, Yp)
	
	
	
measurements = [[(0, 0), 20.2], [(0, 20), 19.2], [(30, 20), 15.8], [(30, 0), 17.0]]
localize_sound_origin(measurements, 2)

